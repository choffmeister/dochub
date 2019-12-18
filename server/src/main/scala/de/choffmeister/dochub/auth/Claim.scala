package de.choffmeister.dochub.auth

import de.choffmeister.dochub.utils.JsonHelpers
import play.api.libs.json.Format

sealed trait Claim {
  override def toString: String = Claim.toString(this)
}
object Claim {
  final case class User(name: String) extends Claim
  final case class Group(name: String) extends Claim
  final case class Unknown(other: String) extends Claim

  def toString(claim: Claim): String = claim match {
    case c: User    => s"user:${c.name}"
    case c: Group   => s"group:${c.name}"
    case c: Unknown => c.other
  }
  def fromString(value: String): Claim = value.split(":", 2).toList match {
    case "user" :: name :: Nil  => User(name)
    case "group" :: name :: Nil => Group(name)
    case other                  => Unknown(other.mkString(":"))
  }
  implicit val format: Format[Claim] = JsonHelpers.mappedFormat[Claim, String](fromString, _.toString)
}
