package de.choffmeister.dochub.data.user

import java.time._

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.dochub.auth.{Claim, Scope}
import de.choffmeister.dochub.data.ExtendedPostgresProfile
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserData(db: ExtendedPostgresProfile#Backend#Database) extends LazyLogging {
  def findUserById(userId: UserId): Future[Option[User]] = {
    val q = userTableQuery.filter(_.id === userId).take(1).result.headOption
    db.run(q)
  }

  def updateUser(
    externalId: String,
    externalData: Option[String],
    username: String,
    claims: Set[Claim]
  ): Future[User] = {
    val q = for {
      existingUser <- userTableQuery.filter(_.externalId === externalId).take(1).result.headOption
      user <- existingUser match {
        case Some(u) =>
          val user = u.copy(
            externalId = externalId,
            externalData = externalData,
            externalLastRefreshedAt = Instant.now,
            username = username,
            claims = claims
          )
          userTableQuery
            .filter(_.id === user.id)
            .map(u => (u.externalId, u.externalData, u.externalLastRefreshedAt, u.username, u.claims))
            .update((user.externalId, user.externalData, user.externalLastRefreshedAt, user.username, user.claims))
            .map(_ => user)
        case None =>
          val user = User(
            externalId = externalId,
            externalData = externalData,
            externalLastRefreshedAt = Instant.now,
            username = username,
            claims = claims
          )
          (userTableQuery += user).map(_ => user)
      }
    } yield user
    db.run(q.transactionally)
  }

  def createApiKey(userId: UserId, name: String, scopes: Set[Scope]): Future[ApiKey] = {
    val apiKey = ApiKey(userId = userId, name = name, scopes = scopes)
    db.run(apiKeyTableQuery += apiKey).map(_ => apiKey)
  }

  def listAllApiKeys(userId: UserId)(page: (Int, Int)): Future[(Seq[ApiKey], Int)] = {
    val q = apiKeyTableQuery.list(_.userId === userId)(page)
    db.run(q)
  }

  def findApiKeyById(apiKeyId: ApiKeyId): Future[Option[ApiKey]] = {
    val q = apiKeyTableQuery.filter(_.id === apiKeyId).take(1).result.headOption
    db.run(q)
  }

  def findApiKeyBySecret(secret: String): Future[Option[ApiKey]] = {
    val q = apiKeyTableQuery.filter(_.secret === secret).take(1).result.headOption
    db.run(q)
  }

  def updateApiKeyLastUsedAt(apiKeyId: ApiKeyId): Future[Done] = {
    val q = apiKeyTableQuery.filter(_.id === apiKeyId).map(_.lastUsedAt).update(Some(Instant.now))
    db.run(q).map(_ => Done)
  }

  def deleteApiKey(apiKeyId: ApiKeyId*): Future[Done] = {
    val q = apiKeyTableQuery.filter(_.id.inSetBind(apiKeyId.toSet)).delete
    db.run(q).map(_ => Done)
  }
}
