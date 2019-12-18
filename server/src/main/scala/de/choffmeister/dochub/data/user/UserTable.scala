package de.choffmeister.dochub.data.user

import java.time._
import java.util.UUID

import de.choffmeister.dochub.auth.Claim
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import play.api.libs.json.{Format, Json, OFormat}
import slick.sql.SqlProfile.ColumnOption.SqlType

private[data] class UserTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[UserId]("id", O.PrimaryKey)
  def username = column[String]("username")
  def claims = column[Set[Claim]]("claims")
  def createdAt = column[Instant]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def externalId = column[String]("external_id")
  def externalData = column[Option[String]]("external_data")
  def externalLastRefreshedAt =
    column[Instant]("external_last_refreshed_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def idxExternalId = index("idx__user__external_id", externalId, unique = true)
  def * =
    (id, username, claims, createdAt, externalId, externalData, externalLastRefreshedAt) <> ((User.apply _).tupled, User.unapply)
}

private[data] object userTableQuery extends TableQuery(new UserTable(_))

final case class UserId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}
object UserId {
  def empty: UserId = fromString("00000000-0000-0000-0000-000000000000")
  def random: UserId = UserId(UUID.randomUUID)
  def fromString(value: String): UserId = UserId(UUID.fromString(value))
  implicit val format: Format[UserId] = Json.valueFormat[UserId]
}

final case class User(
  id: UserId = UserId.random,
  username: String,
  claims: Set[Claim],
  createdAt: Instant = Instant.now,
  externalId: String,
  externalData: Option[String] = None,
  externalLastRefreshedAt: Instant
)
object User {
  implicit val format: OFormat[User] = Json.format[User]
}
