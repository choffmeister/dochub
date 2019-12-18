package de.choffmeister.dochub.connectors.ftp

import java.io.{InputStream, OutputStream}
import java.util

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import akka.util.ByteString
import org.apache.ftpserver.ftplet._
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager._
import org.apache.ftpserver.{DataConnectionConfigurationFactory, FtpServerFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class UploadFtpServer[T](
  port: Int,
  passiveAddress: String,
  passivePorts: (Int, Int),
  authorize: (String, String) => Future[Option[T]],
  upload: (T, String) => Sink[ByteString, _]
)(implicit ec: ExecutionContext, mat: Materializer) {
  require(passivePorts._1 >= 1024, "Invalid passive port range")
  require(passivePorts._1 <= passivePorts._2, "Invalid passive port range")

  def start(): Future[() => Unit] = Future {
    val serverFactory = new FtpServerFactory()

    val factory = new ListenerFactory()
    factory.setPort(port)
    serverFactory.addListener("default", factory.createListener)

    val dataConnectionConfigurationFactory = new DataConnectionConfigurationFactory()
    dataConnectionConfigurationFactory.setPassiveAddress(passiveAddress)
    dataConnectionConfigurationFactory.setPassiveExternalAddress(passiveAddress)
    dataConnectionConfigurationFactory.setPassivePorts(s"${passivePorts._1}-${passivePorts._2}")
    factory.setDataConnectionConfiguration(dataConnectionConfigurationFactory.createDataConnectionConfiguration())

    serverFactory.setFileSystem(new FileSystemFactory {
      override def createFileSystemView(user: User): FileSystemView = new FileSystemView {
        override def getHomeDirectory: FtpFile = VirtualFtpDirectory
        override def getWorkingDirectory: FtpFile = VirtualFtpDirectory
        override def changeWorkingDirectory(dir: String): Boolean = dir match {
          case "/"  => true
          case "./" => true
          case _    => false
        }
        override def getFile(file: String): FtpFile = file match {
          case "/"  => VirtualFtpDirectory
          case "./" => VirtualFtpDirectory
          case file =>
            VirtualFtpFile(file, name => {
              upload(user.asInstanceOf[VirtualFtpUser].data, name)
            })
        }
        override def isRandomAccessible: Boolean = true
        override def dispose(): Unit = {}
      }
    })

    serverFactory.setUserManager(new UserManager {
      override def getUserByName(username: String): User = null

      override def getAllUserNames: Array[String] = new Array[String](0)

      override def delete(username: String): Unit = throw new UnsupportedOperationException()

      override def save(user: User): Unit = throw new UnsupportedOperationException()

      override def doesExist(username: String): Boolean = username == "api"

      override def authenticate(authentication: Authentication): User =
        (authentication match {
          case upa: UsernamePasswordAuthentication =>
            Await.result(authorize(upa.getUsername, upa.getPassword), 3.seconds).map(VirtualFtpUser.apply)
          case _ => None
        }).orNull

      override def getAdminName: String = throw new FtpException()

      override def isAdmin(username: String): Boolean = false
    })

    val server = serverFactory.createServer()
    server.start()
    () => server.stop()
  }

  case class VirtualFtpUser(data: T) extends User {
    override def getName: String = ""
    override def getPassword: String = ""
    override def getAuthorities: util.List[_ <: Authority] = ???
    override def getAuthorities(clazz: Class[_ <: Authority]): util.List[_ <: Authority] = ???
    override def authorize(request: AuthorizationRequest): AuthorizationRequest = request
    override def getMaxIdleTime: Int = 60
    override def getEnabled: Boolean = true
    override def getHomeDirectory: String = "/"
  }

  case object VirtualFtpDirectory extends FtpFile {
    override def getAbsolutePath: String = "/"
    override def getName: String = "/"
    override def isHidden: Boolean = false
    override def isDirectory: Boolean = true
    override def isFile: Boolean = false
    override def doesExist(): Boolean = true
    override def isReadable: Boolean = true
    override def isWritable: Boolean = true
    override def isRemovable: Boolean = false
    override def getOwnerName: String = "user"
    override def getGroupName: String = "user"
    override def getLinkCount: Int = 3
    override def getLastModified: Long = 0L
    override def setLastModified(time: Long): Boolean = false
    override def getSize: Long = 0L
    override def getPhysicalFile: AnyRef = ???
    override def mkdir(): Boolean = false
    override def delete(): Boolean = false
    override def move(destination: FtpFile): Boolean = false
    override def listFiles(): util.List[_ <: FtpFile] = new util.LinkedList[VirtualFtpFile]()
    override def createOutputStream(offset: Long): OutputStream = ???
    override def createInputStream(offset: Long): InputStream = ???
  }

  case class VirtualFtpFile(name: String, upload: String => Sink[ByteString, _])(
    implicit ec: ExecutionContext,
    mat: Materializer
  ) extends FtpFile {
    override def getAbsolutePath: String = s"/$name"
    override def getName: String = name
    override def isHidden: Boolean = false
    override def isDirectory: Boolean = false
    override def isFile: Boolean = true
    override def doesExist(): Boolean = ???
    override def isReadable: Boolean = true
    override def isWritable: Boolean = true
    override def isRemovable: Boolean = false
    override def getOwnerName: String = "user"
    override def getGroupName: String = "user"
    override def getLinkCount: Int = 1
    override def getLastModified: Long = 0L
    override def setLastModified(time: Long): Boolean = false
    override def getSize: Long = 0L
    override def getPhysicalFile: AnyRef = ???
    override def mkdir(): Boolean = false
    override def delete(): Boolean = false
    override def move(destination: FtpFile): Boolean = false
    override def listFiles(): util.List[_ <: FtpFile] = ???
    override def createOutputStream(offset: Long): OutputStream = {
      StreamConverters
        .asOutputStream()
        .to(upload(name.stripPrefix("/")))
        .run()
    }
    override def createInputStream(offset: Long): InputStream = ???
  }
}
