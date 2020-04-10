package de.choffmeister.dochub.data.document

import java.util.UUID

import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data.user.UserId
import org.apache.commons.codec.binary.Hex
import play.api.libs.json._
import slick.lifted.CanBeQueryCondition

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

private[data] class LabelTable(tag: Tag) extends Table[Label](tag, "labels") {
  def id = column[LabelId]("id")
  def userId = column[UserId]("user_id")
  def name = column[String]("name")
  def color = column[Color]("color")
//  def fkUserId =
//    foreignKey("fk__label__user_id", userId, userTableQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  def idxName = index("idx__label__name", name)
  def * = (id, userId, name, color) <> ((Label.apply _).tupled, Label.unapply)
}

private[data] object labelTableQuery extends TableQuery(new LabelTable(_)) {
  type ListFilter = LabelTable

  def list[T <: Rep[_]](userId: UserId, filter: ListFilter => T)(
    page: (Int, Int)
  )(implicit wt: CanBeQueryCondition[T], ec: ExecutionContext) = {
    val q = labelTableQuery
      .filter(_.userId === userId)
      .filter(filter)
      .sortBy(_.name.asc)
    for {
      labels <- q
        .drop(page._1)
        .take(page._2)
        .result
      totalCount <- q.size.result
    } yield (labels, totalCount)
  }
}

final case class LabelId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}
object LabelId {
  def empty: LabelId = fromString("00000000-0000-0000-0000-000000000000")
  def random: LabelId = LabelId(UUID.randomUUID)
  def fromString(value: String): LabelId = LabelId(UUID.fromString(value))
  implicit val format: Format[LabelId] = Json.valueFormat[LabelId]
}

final case class Color(red: Byte, green: Byte, blue: Byte) {
  override def toString: String = "#" + Hex.encodeHexString(List(red, green, blue).toArray)
}
object Color {
  private val regex = """#([0-9a-f]{6})""".r
  def fromString(value: String): Color = {
    value match {
      case regex(hex) =>
        val array = Hex.decodeHex(hex)
        Color(array(0), array(1), array(2))
      case other => throw new RuntimeException(s"Unable to parse '$other' to color")
    }
  }
  implicit val format: Format[Color] = new Format[Color] {
    override def writes(o: Color): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[Color] = json match {
      case JsString(hex) =>
        Try(Color.fromString(hex)) match {
          case Success(color) => JsSuccess(color)
          case Failure(_)     => JsError("Expected a color string")
        }
      case _ =>
        JsError("Expected a string")
    }
  }
}

final case class Label(id: LabelId, userId: UserId, name: String, color: Color)
object Label {
  implicit val format: OFormat[Label] = Json.format[Label]
}
