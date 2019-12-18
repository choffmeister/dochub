package de.choffmeister.dochub

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.softwaremill.macwire._
import de.choffmeister.dochub.auth.{AuthProvider, GitHubGrant}
import de.choffmeister.dochub.connectors.ftp.FtpConnector
import de.choffmeister.dochub.data.document.DocumentData
import de.choffmeister.dochub.data.user.UserData
import de.choffmeister.dochub.data.{DataCleanup, DataInit}
import de.choffmeister.dochub.http._
import de.choffmeister.dochub.utils.OcrmypdfClient
import de.choffmeister.microserviceutils.ServiceBase
import de.choffmeister.microserviceutils.components.{BaseComponents, DefaultBaseComponents}
import de.choffmeister.microserviceutils.http.JsonWebTokenAuthenticator

import scala.concurrent.Future

trait ServiceComponents {
  this: BaseComponents with DatabaseComponents =>
  lazy val apiKeyRoutes: ApiKeyRoutes = wire[ApiKeyRoutes]
  lazy val authProvider: AuthProvider = wireWith(AuthProvider.fromConfig _)
  lazy val authenticator: JsonWebTokenAuthenticator = wire[JsonWebTokenAuthenticator]
  lazy val documentData: DocumentData = wireWith(DocumentData.fromConfig _)
  lazy val documentRoutes: RoutesGroup = wire[DocumentRoutes]
  lazy val ftpConnector: FtpConnector = wireWith(FtpConnector.fromConfig _)
  lazy val gitHubGrant: GitHubGrant = wire[GitHubGrant]
  lazy val httpServer: HttpServer = wire[HttpServer]
  lazy val ocrmypdfClient: OcrmypdfClient = wireWith(OcrmypdfClient.fromConfig _)
  lazy val routes: Set[RoutesGroup] = wireSet[RoutesGroup]
  lazy val staticContentRoutes: StaticContentRoutes = wire[StaticContentRoutes]
  lazy val userData: UserData = wire[UserData]
}

class Service extends ServiceBase with DefaultBaseComponents with DefaultDatabaseComponents with ServiceComponents {
  override implicit lazy val actorSystem = ActorSystem("dochub")
  override implicit lazy val executionContext = actorSystem.dispatcher
  override implicit lazy val materializer = ActorMaterializer()(actorSystem)

  override def init(): Future[Done] = {
    for {
      _ <- DataInit.run(database)
      _ <- DataCleanup.run(database, Paths.get(config.getString("storage.directory")))
      _ <- ftpConnector.init()
    } yield Done
  }
}
