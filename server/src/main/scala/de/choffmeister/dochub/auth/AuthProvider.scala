package de.choffmeister.dochub.auth

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.stream.Materializer
import com.typesafe.config.Config
import de.choffmeister.dochub.data.user.{User, UserData, UserId}
import de.choffmeister.microserviceutils.apis.authtoken.AuthTokenApi.AuthResult
import de.choffmeister.microserviceutils.apis.authtoken.{AuthTokenApi, AuthProvider => AuthProviderBase}
import de.choffmeister.microserviceutils.http.JsonWebTokenAuthenticator
import io.jsonwebtoken.{Claims, Jwts}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

final case class Principal(userId: UserId, username: String, claims: Set[Claim], scopes: Set[Scope])
object Principal {
  def fromUser(user: User): Principal =
    Principal(userId = user.id, username = user.username, claims = user.claims, scopes = Scope.all)
  val unknown: Principal = Principal(userId = UserId.empty, username = "", claims = Set.empty, scopes = Set.empty)
}

final case class ExternalPrincipal(id: String, data: Option[String], username: String, claims: Set[Claim])

class AuthProvider(
  override val authenticator: JsonWebTokenAuthenticator,
  externalGrant: AuthExternalGrant[UserId, ExternalPrincipal, NotUsed],
  userData: UserData,
  claimFilter: Set[Claim]
)(implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
    extends AuthProviderBase[Principal, NotUsed] {
  val externalLifeTime = 1.day

  def routes: Route =
    concat(path("token" / "create")(AuthTokenApi(this)), extractExtraInformation {
      extraInformation =>
        externalGrants.foldLeft[Route](reject)((acc, eg) => {
          concat(acc, pathPrefix(eg.`type`) {
            eg.loginRoutes(extraInformation, principal => {
              if (principal.claims.exists(claimFilter.contains)) {
                userData
                  .updateUser(s"${eg.`type`}:${principal.id}", principal.data, principal.username, principal.claims)
                  .map { user =>
                    val internalPrincipal = Principal.fromUser(user)
                    val refreshToken = authenticator
                      .createBearerToken(principalToClaims(internalPrincipal).setAudience("refresh"), Some(30.seconds))
                    Right(refreshToken)
                  }
              } else {
                Future.successful(Left("Access denied due to lack of permission."))
              }
            })
          })
        })
    })

  override val grants = List.empty

  val externalGrants: List[AuthExternalGrant[UserId, ExternalPrincipal, NotUsed]] = List(externalGrant)

  override def principalToClaims(principal: Principal): Claims = {
    val map = new java.util.LinkedHashMap[String, Object]()
    map.put("sub", principal.userId.toString)
    map.put("userId", principal.userId.toString)
    map.put("username", principal.username)
    map.put("claims", principal.claims.map(_.toString).mkString(","))
    map.put("scopes", principal.scopes.map(_.toString).mkString(","))
    Jwts.claims(map)
  }

  override def claimsToPrincipal(jwt: Claims): AuthResult[Principal] = {
    val userId =
      Option(jwt.get("sub", classOf[String])).orElse(Option(jwt.get("userId", classOf[String]))).map(UserId.fromString)
    val username = Option(jwt.get("username", classOf[String]))
    val claims = Option(jwt.get("claims", classOf[String])).map(_.split(",").map(Claim.fromString).toSet)
    val scopes = Option(jwt.get("scopes", classOf[String])).map(_.split(",").map(Scope.fromString).toSet)
    val markers = Option(jwt.get("markers", classOf[String])).map(_.split(",").toSet).getOrElse(Set.empty)

    (userId, username, claims, scopes) match {
      case (Some(sub), Some(username), Some(claims), Some(scopes)) =>
        Right(Principal(userId = sub, username = username, claims = claims, scopes = scopes))
      case _ =>
        Left(AuthTokenApi.InvalidCredentials)
    }
  }

  override def refreshPrincipal(principal: Principal, issuedAt: Instant): Future[AuthResult[Principal]] = {
    userData.findUserById(principal.userId).flatMap {
      case Some(user) =>
        refreshUserExternallyIfNeeded(user).map {
          case refreshedUser if refreshedUser.claims.exists(claimFilter.contains) =>
            Right(
              Principal(
                userId = refreshedUser.id,
                username = refreshedUser.username,
                claims = refreshedUser.claims,
                scopes = principal.scopes
              )
            )
          case _ =>
            Left(AuthTokenApi.InvalidCredentials)
        }
      case None =>
        Future.successful(Left(AuthTokenApi.InvalidCredentials))
    }
  }

  private def refreshUserExternallyIfNeeded(user: User): Future[User] = {
    val externalType = user.externalId.split(":", 2).head
    val externalGrant = externalGrants.find(_.`type` == externalType).get

    if (user.externalLastRefreshedAt.isBefore(Instant.now.minusSeconds(externalLifeTime.toSeconds))) {
      val externalPrincipal = ExternalPrincipal(
        id = user.externalId.split(":", 2).tail.head,
        data = user.externalData,
        username = user.username,
        claims = user.claims
      )

      for {
        principal <- externalGrant.refreshPrincipal(externalPrincipal)
        user <- userData.updateUser(
          s"${externalGrant.`type`}:${principal.id}",
          principal.data,
          principal.username,
          principal.claims
        )
      } yield user
    } else Future.successful(user)
  }

  override def extractExtraInformation: Directive1[NotUsed] = provide(NotUsed)

  private val defaultExtractToken: Directive1[Option[String]] = parameter('token.?).flatMap {
    case Some(token) => provide(Some(token))
    case None =>
      extractCredentials.flatMap {
        case Some(OAuth2BearerToken(token)) => provide(Some(token))
        case _                              => provide(None)
      }
  }

  def authenticateWeb(
    extractToken: Directive1[Option[String]] = defaultExtractToken
  ): AuthenticationDirective[Principal] = {
    val inner: Directive1[Principal] = extractToken.flatMap {
      case Some(token) =>
        onSuccess(authenticator.validateToken("Bearer", token, acceptExpiredToken = false)).flatMap {
          case Right(claims) =>
            claimsToPrincipal(claims) match {
              case Right(principal) =>
                provide(principal)
              case Left(err) =>
                val challenge = HttpChallenge(
                  "Bearer",
                  authenticator.realm,
                  Map("error" -> err.error, "error_description" -> err.description)
                )
                reject(AuthenticationFailedRejection(CredentialsRejected, challenge))
            }
          case Left(challenge) =>
            reject(AuthenticationFailedRejection(CredentialsRejected, challenge))
        }
      case None =>
        val challenge = HttpChallenge("Bearer", authenticator.realm, Map("error" -> "token_missing"))
        reject(AuthenticationFailedRejection(CredentialsMissing, challenge))
    }
    AuthenticationDirective(inner)
  }
}

object AuthProvider {
  def fromConfig(
    config: Config,
    authenticator: JsonWebTokenAuthenticator,
    externalGrant: AuthExternalGrant[UserId, ExternalPrincipal, NotUsed],
    userData: UserData,
    system: ActorSystem,
    executor: ExecutionContext,
    materializer: Materializer
  ): AuthProvider = {
    val claimFilter = config.getStringList("claim-filter").asScala.map(Claim.fromString).toSet
    new AuthProvider(authenticator, externalGrant, userData, claimFilter)(system, executor, materializer)
  }
}

trait AuthExternalGrant[I, P, E] {
  val `type`: String
  def loginRoutes(extraInformation: E, createInitialRefreshToken: P => Future[Either[String, String]]): Route
  def refreshPrincipal(principal: P): Future[P]
}
