package de.choffmeister.dochub.data.user

import java.util.UUID

import play.api.libs.json.{Format, Json}

final case class UserId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}
object UserId {
  def empty: UserId = fromString("00000000-0000-0000-0000-000000000000")
  def random: UserId = UserId(UUID.randomUUID)
  def fromString(value: String): UserId = UserId(UUID.fromString(value))
  implicit val format: Format[UserId] = Json.valueFormat[UserId]
}
