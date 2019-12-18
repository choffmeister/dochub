package de.choffmeister.dochub.connectors.ftp

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.stream.Materializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.dochub.auth.Scope
import de.choffmeister.dochub.connectors.Connector
import de.choffmeister.dochub.data.document.DocumentData
import de.choffmeister.dochub.data.user.{ApiKey, UserData}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

final case class FtpConnectorError(message: String) extends RuntimeException(message) with NoStackTrace

class FtpConnector(
  port: Int,
  passiveAddress: String,
  passivePorts: (Int, Int),
  userData: UserData,
  documentData: DocumentData
)(implicit sys: ActorSystem, mat: Materializer, ec: ExecutionContext)
    extends Connector
    with LazyLogging {
  private val resolver = ContentTypeResolver.Default
  private val ftpServer = new UploadFtpServer[ApiKey](port, passiveAddress, passivePorts, (username, password) => {
    if (username == "api") userData.findApiKeyBySecret(password)
    else Future.successful(None)
  }, (apiKey, name) => {
    if (apiKey.scopes.contains(Scope.Write)) {
      logger.info(s"Uploading $name")
      documentData
        .putBlob()
        .mapMaterializedValue(_.flatMap { blob =>
          val contentType = resolver(name)
          documentData.createDocument(apiKey.userId, name, Set.empty, blob.id, contentType)
        })
    } else {
      logger.warn(s"Uploading $name rejected due to lack of permissions")
      throw FtpConnectorError("Insufficient permissions")
    }
  })

  override def init(): Future[Done] = ftpServer.start().map(_ => Done)
}

object FtpConnector {
  def fromConfig(
    config: Config,
    userData: UserData,
    documentData: DocumentData,
    sys: ActorSystem,
    mat: Materializer,
    ec: ExecutionContext
  ): FtpConnector = {
    val ftpConfig = config.getConfig("connectors.ftp")
    val port = ftpConfig.getInt("port")
    val passiveAddress = ftpConfig.getString("passive.address")
    val passivePorts = (
      ftpConfig.getStringList("passive.ports").asScala(0).toInt,
      ftpConfig.getStringList("passive.ports").asScala(1).toInt
    )
    new FtpConnector(port, passiveAddress, passivePorts, userData, documentData)(sys, mat, ec)
  }
}
