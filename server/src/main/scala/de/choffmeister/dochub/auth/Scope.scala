package de.choffmeister.dochub.auth

import de.choffmeister.dochub.utils.JsonHelpers
import play.api.libs.json.Format

sealed trait Scope {
  override def toString: String = Scope.toString(this)
}
object Scope {
  case object Admin extends Scope
  case object Write extends Scope
  case object Read extends Scope
  final case class Unknown(other: String) extends Scope

  def all: Set[Scope] = Set(Admin, Write, Read)

  def toString(scope: Scope): String = scope match {
    case Admin      => "admin"
    case Write      => "write"
    case Read       => "read"
    case s: Unknown => s.other
  }
  def fromString(str: String): Scope = str match {
    case "admin" => Admin
    case "write" => Write
    case "read"  => Read
    case other   => Unknown(other)
  }
  implicit val format: Format[Scope] = JsonHelpers.mappedFormat[Scope, String](fromString, _.toString)
}
