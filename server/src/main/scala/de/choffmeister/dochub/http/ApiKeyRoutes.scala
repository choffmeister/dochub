package de.choffmeister.dochub.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.stream.Materializer
import de.choffmeister.dochub.auth.{AuthProvider, Scope}
import de.choffmeister.dochub.data.user.{ApiKeyId, UserData}
import de.choffmeister.dochub.utils.AkkaHttpHelpers

import scala.concurrent.ExecutionContext

class ApiKeyRoutes(authProvider: AuthProvider, userData: UserData)(
  implicit system: ActorSystem,
  executionContext: ExecutionContext,
  materializer: Materializer
) extends RoutesGroup {
  def listApiKeysRoute: Route = (path("api" / "api-keys") & get) {
    authProvider.authenticateWeb() { principal =>
      AkkaHttpHelpers.paging() { page =>
        val future = for {
          (apiKeys, totalCount) <- userData.listAllApiKeys(principal.userId)(page)
        } yield (apiKeys, totalCount)
        onSuccess(future) {
          case (apiKeys, totalCount) =>
            val page = HalPage
              .from(apiKeys.map(_.stripSecret), Some(totalCount))
            complete(page)
        }
      }
    }
  }

  def createApiKeyRoute: Route = (path("api" / "api-keys") & post) {
    parameters('name, 'scopes.*) {
      case (name, scopesRaw) =>
        val scopes = scopesRaw.map(Scope.fromString).toSet
        authProvider.authenticateWeb() { principal =>
          complete(userData.createApiKey(principal.userId, name, scopes))
        }
    }
  }

  def deleteApiKeyRoute: Route = (path("api" / "api-keys" / JavaUUID.map(ApiKeyId.apply)) & delete) { apiKeyId =>
    authProvider.authenticateWeb() { principal =>
      onSuccess(userData.findApiKeyById(apiKeyId)) {
        case Some(apiKey) if apiKey.userId == principal.userId =>
          complete(userData.deleteApiKey(apiKeyId))
        case _ =>
          reject
      }
    }
  }

  def routes: Route = concat(listApiKeysRoute, createApiKeyRoute, deleteApiKeyRoute)
}
