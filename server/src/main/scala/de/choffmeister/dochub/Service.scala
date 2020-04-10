package de.choffmeister.dochub

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import com.softwaremill.macwire._
import de.choffmeister.dochub.auth.AuthConsumer
import de.choffmeister.dochub.connectors.ftp.FtpConnector
import de.choffmeister.dochub.data.document.DocumentData
import de.choffmeister.dochub.data.{DataCleanup, DataInit}
import de.choffmeister.dochub.http._
import de.choffmeister.dochub.utils.OcrmypdfClient
import de.choffmeister.microserviceutils.ServiceBase
import de.choffmeister.microserviceutils.components.{BaseComponents, DefaultBaseComponents}

import scala.concurrent.Future

trait ServiceComponents {
  this: BaseComponents with DatabaseComponents =>
  lazy val authConsumer: AuthConsumer = wire[AuthConsumer]
  lazy val documentData: DocumentData = wireWith(DocumentData.fromConfig _)
  lazy val documentRoutes: DocumentRoutes = wire[DocumentRoutes]
  lazy val ftpConnector: FtpConnector = wireWith(FtpConnector.fromConfig _)
  lazy val httpServer: HttpServer = wire[HttpServer]
  lazy val httpServerRoutes: Set[HttpServerRoutes] = wireSet[HttpServerRoutes]
  lazy val ocrmypdfClient: OcrmypdfClient = wireWith(OcrmypdfClient.fromConfig _)
  lazy val staticContentRoutes: StaticContentRoutes = wire[StaticContentRoutes]
}

class Service extends ServiceBase with DefaultBaseComponents with DefaultDatabaseComponents with ServiceComponents {
  override implicit lazy val actorSystem = ActorSystem("dochub")
  override implicit lazy val executionContext = actorSystem.dispatcher

  override def init(): Future[Done] = {
    for {
      _ <- DataInit.run(database)
      _ <- DataCleanup.run(database, Paths.get(config.getString("storage.directory")))
      _ <- ftpConnector.init()
    } yield Done
  }
}
