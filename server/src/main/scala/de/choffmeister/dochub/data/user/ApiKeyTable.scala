package de.choffmeister.dochub.data.user

import java.security.SecureRandom
import java.time._
import java.util.UUID

import de.choffmeister.dochub.auth.Scope
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.utils.JsonHelpers
import org.apache.commons.codec.binary.Hex
import play.api.libs.json.{Format, Json}
import slick.lifted.CanBeQueryCondition
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.ExecutionContext

private[data] class ApiKeyTable(tag: Tag) extends Table[ApiKey](tag, "api_keys") {
  def id = column[ApiKeyId]("id", O.PrimaryKey)
  def userId = column[UserId]("user_id")
  def name = column[String]("name")
  def scopes = column[Set[Scope]]("scopes")
  def secret = column[String]("secret")
  def createdAt = column[Instant]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def lastUsedAt = column[Option[Instant]]("last_used_at", SqlType("timestamp"))
  def idxSecret = index("idx_api_key_secret", secret, unique = true)
  def fkUser =
    foreignKey("fk_api_key_user_id_user_id", userId, userTableQuery)(
      _.id,
      ForeignKeyAction.Cascade,
      ForeignKeyAction.Cascade
    )
  def * =
    (id, userId, name, scopes, secret, createdAt, lastUsedAt) <> ((ApiKey.apply _).tupled, ApiKey.unapply)
}
private[data] object apiKeyTableQuery extends TableQuery(new ApiKeyTable(_)) {
  type ListFilter = ApiKeyTable

  def list[T <: Rep[_]](
    filter: ListFilter => T
  )(page: (Int, Int))(implicit wt: CanBeQueryCondition[T], ec: ExecutionContext) = {
    val q1 = apiKeyTableQuery
      .filter(filter)
      .sortBy(ak => (ak.lastUsedAt.desc.nullsLast, ak.createdAt.desc))
    for {
      items <- q1.drop(page._1).take(page._2).result
      totalCount <- q1.size.result
    } yield (items, totalCount)
  }
}

final case class ApiKeyId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}
object ApiKeyId {
  def empty: ApiKeyId = fromString("00000000-0000-0000-0000-000000000000")
  def random: ApiKeyId = ApiKeyId(UUID.randomUUID)
  def fromString(value: String): ApiKeyId = ApiKeyId(UUID.fromString(value))
  implicit val format: Format[ApiKeyId] = JsonHelpers.mappedFormat[ApiKeyId, String](fromString, _.toString)
}

final case class ApiKey(
  id: ApiKeyId = ApiKeyId.random,
  userId: UserId,
  name: String,
  scopes: Set[Scope],
  secret: String = ApiKey.generateSecret(),
  createdAt: Instant = Instant.now,
  lastUsedAt: Option[Instant] = None
) {
  def stripSecret: ApiKey = copy(secret = "***")
}
object ApiKey {
  private def rnd = new SecureRandom()
  def generateSecret(): String = {
    val bytes = new Array[Byte](16)
    rnd.nextBytes(bytes)
    Hex.encodeHexString(bytes)
  }
  implicit val format: Format[ApiKey] = Json.format[ApiKey]
}
