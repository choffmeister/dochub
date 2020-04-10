package de.choffmeister.dochub.connectors.ftp

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.ContentTypeResolver
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.dochub.connectors.Connector
import de.choffmeister.dochub.data.document.DocumentData
import de.choffmeister.dochub.data.user.UserId

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

final case class FtpConnectorError(message: String) extends RuntimeException(message) with NoStackTrace

class FtpConnector(port: Int, passiveAddress: String, passivePorts: (Int, Int), documentData: DocumentData)(
  implicit sys: ActorSystem,
  ec: ExecutionContext
) extends Connector
    with LazyLogging {
  private val resolver = ContentTypeResolver.Default
  private val ftpServer = new UploadFtpServer[NotUsed](port, passiveAddress, passivePorts, (username, password) => {
    Future.successful(Some(NotUsed))
  }, (_, name) => {
    logger.info(s"Uploading $name")
    documentData
      .putBlob()
      .mapMaterializedValue(_.flatMap { blob =>
        val contentType = resolver(name)
        documentData.createDocument(UserId.empty, name, Set.empty, blob.id, contentType)
      })
//      logger.warn(s"Uploading $name rejected due to lack of permissions")
//      throw FtpConnectorError("Insufficient permissions")
  })

  override def init(): Future[Done] = ftpServer.start().map(_ => Done)
}

object FtpConnector {
  def fromConfig(config: Config, documentData: DocumentData, sys: ActorSystem, ec: ExecutionContext): FtpConnector = {
    val ftpConfig = config.getConfig("connectors.ftp")
    val port = ftpConfig.getInt("port")
    val passiveAddress = ftpConfig.getString("passive.address")
    val passivePorts = (
      ftpConfig.getStringList("passive.ports").asScala(0).toInt,
      ftpConfig.getStringList("passive.ports").asScala(1).toInt
    )
    new FtpConnector(port, passiveAddress, passivePorts, documentData)(sys, ec)
  }
}
