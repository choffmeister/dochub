package de.choffmeister.dochub.auth

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, Link, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import de.choffmeister.dochub.data.user.UserId
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class GitHubGrant(config: Config)(implicit system: ActorSystem, executor: ExecutionContext, materializer: Materializer)
    extends GitHubGrantBase[UserId, ExternalPrincipal, NotUsed] {
  override val clientId: String = config.getString("github.client-id")
  override val clientSecret: String = config.getString("github.client-secret")
  override val callbackUri: Uri = Uri(config.getString("http.base-uri")) + s"/api/auth/${`type`}/callback"
  override val returnUri: Uri = Uri(config.getString("http.base-uri"))

  override def getPrincipalFromAccessToken(accessToken: String): Future[ExternalPrincipal] = {
    for {
      user <- getUser(accessToken)
      orgs <- getOrganizations(accessToken, user.login).runWith(Sink.seq).map(_.toList)
    } yield ExternalPrincipal(
      id = user.id.toString,
      data = Some(accessToken),
      username = user.login,
      claims = Set(Claim.User(user.login)) ++ orgs.map(org => Claim.Group(org.login)).toSet
    )
  }

  override def refreshPrincipal(principal: ExternalPrincipal): Future[ExternalPrincipal] = {
    getPrincipalFromAccessToken(principal.data.get)
  }
}

abstract class GitHubGrantBase[I, P, E]()(
  implicit system: ActorSystem,
  executor: ExecutionContext,
  materializer: Materializer
) extends AuthExternalGrant[I, P, E] {
  override val `type`: String = "github"

  val clientId: String
  val clientSecret: String
  val callbackUri: Uri
  val returnUri: Uri

  def loginRoutes(extraInformation: E, createInitialRefreshToken: P => Future[Either[String, String]]): Route =
    concat(
      pathEnd {
        val uri = Uri("https://github.com/login/oauth/authorize").withQuery(
          Uri
            .Query("client_id" -> clientId, "state" -> UUID.randomUUID.toString, "redirect_uri" -> callbackUri.toString)
        )
        redirect(uri, StatusCodes.Found)
      },
      path("callback") {
        parameter('code, 'state) {
          case (code, state) =>
            val refreshToken = for {
              accessToken <- exchangeCodeForAccessToken(code, state)
              principal <- getPrincipalFromAccessToken(accessToken)
              refreshToken <- createInitialRefreshToken(principal)
            } yield refreshToken
            onSuccess(refreshToken) {
              case Right(refreshToken) =>
                redirect(returnUri.withQuery(Uri.Query("refreshToken" -> refreshToken)), StatusCodes.Found)
              case Left(message) =>
                redirect(returnUri.withQuery(Uri.Query("message" -> message)), StatusCodes.Found)
            }
        }
      }
    )

  def getPrincipalFromAccessToken(accessToken: String): Future[P]

  protected def exchangeCodeForAccessToken(code: String, state: String): Future[String] = {
    for {
      response <- Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = Uri("https://github.com/login/oauth/access_token").withQuery(
            Uri.Query("client_id" -> clientId, "client_secret" -> clientSecret, "code" -> code, "state" -> state)
          )
        )
      )
      entity <- response.entity.toStrict(3.second)
      data = Uri.Query(entity.data.utf8String)
    } yield data.get("access_token").get
  }

  protected def getUser(accessToken: String): Future[GitHubUser] = {
    implicit val gitHubUserFormat = GitHubUser.format
    for {
      response <- Http().singleRequest(
        HttpRequest(
          method = HttpMethods.GET,
          uri = Uri("https://api.github.com/user"),
          headers = Vector(Authorization(OAuth2BearerToken(accessToken)))
        )
      )
      entity <- response.entity.toStrict(3.second)
    } yield Json.parse(entity.data.utf8String).as[GitHubUser]
  }

  protected def getOrganizations(accessToken: String, login: String): Source[GitHubOrganization, NotUsed] = {
    implicit val gitHubOrganizationFormat = GitHubOrganization.format
    stream(accessToken, Uri(s"https://api.github.com/users/$login/orgs?per_page=100"))
  }

  protected def getRepositories(accessToken: String): Source[GitHubRepository, NotUsed] = {
    implicit val gitHubRepositoryFormat = GitHubRepository.format
    stream(accessToken, Uri("https://api.github.com/user/repos?per_page=100"))
  }

  protected def stream[T](accessToken: String, startUri: Uri)(implicit format: OFormat[T]): Source[T, NotUsed] = {
    Source
      .unfoldAsync[Option[Uri], Vector[T]](Some(startUri)) {
        case Some(uri) =>
          for {
            response <- Http().singleRequest(
              HttpRequest(
                method = HttpMethods.GET,
                uri = uri,
                headers = Vector(Authorization(OAuth2BearerToken(accessToken)))
              )
            )
            entity <- response.entity.toStrict(3.second)
            json = Json.parse(entity.data.utf8String)
            links = response.header[Link]
            nextUri = links.flatMap(_.values.find(_.params.exists(p => p.key == "rel" && p.value == "next")).map(_.uri))
          } yield Some((nextUri, json.as[Vector[T]]))
        case None =>
          Future.successful(None)
      }
      .mapConcat(identity)
  }
}

final case class GitHubUser(id: Long, login: String, email: Option[String])
object GitHubUser {
  implicit val format = Json.format[GitHubUser]
}

final case class GitHubOrganization(id: Long, login: String)
object GitHubOrganization {
  implicit val format = Json.format[GitHubOrganization]
}

final case class GitHubRepositoryPermissions(admin: Boolean, push: Boolean, pull: Boolean)
object GitHubRepositoryPermissions {
  implicit val format = Json.format[GitHubRepositoryPermissions]
}

final case class GitHubRepository(id: Long, name: String, full_name: String, permissions: GitHubRepositoryPermissions)
object GitHubRepository {
  implicit val format = Json.format[GitHubRepository]
}
