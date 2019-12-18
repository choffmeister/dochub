package de.choffmeister.dochub.http

import java.nio.file.Files

import akka.NotUsed
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.softwaremill.macwire._
import de.choffmeister.dochub.DatabaseTest
import de.choffmeister.dochub.auth._
import de.choffmeister.dochub.data.document.DocumentData
import de.choffmeister.dochub.data.user.{User, UserData, UserId}
import de.choffmeister.microserviceutils.http.JsonWebTokenAuthenticator
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

class HttpServerTest extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with PlayJsonSupport {
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(10, Millis))
  implicit val timeout = RouteTestTimeout(15.seconds)

  "works" in DatabaseTest { db =>
    val basePath = Files.createTempDirectory("dochub")
    val documentData = new DocumentData(db, basePath)
    val userData = new UserData(db)
    val user1 = userData.updateUser("github:u1", None, "u1", Set.empty).futureValue
    val user2 = userData.updateUser("github:u2", None, "u2", Set.empty).futureValue

    val externalGrant = new AuthExternalGrant[UserId, ExternalPrincipal, NotUsed] {
      override val `type`: String = "test"
      override def loginRoutes(ei: NotUsed, cirt: ExternalPrincipal => Future[Either[String, String]]): Route = reject
      override def refreshPrincipal(p: ExternalPrincipal): Future[ExternalPrincipal] = Future.successful(p)
    }
    val authenticator = new JsonWebTokenAuthenticator()
    val authProvider = new AuthProvider(authenticator, externalGrant, userData, Set.empty)
    def creds(user: User) = {
      val principal = Principal(user.id, user.username, Set.empty, Scope.all)
      addCredentials(OAuth2BearerToken(authenticator.createBearerToken(authProvider.principalToClaims(principal))))
    }

    val staticContentRoutes: StaticContentRoutes = wire[StaticContentRoutes]
    val routeGroups: Set[RoutesGroup] = wireSet[RoutesGroup]
    val httpServer = new HttpServer(authProvider, staticContentRoutes, routeGroups)

  // TODO
  }
}
