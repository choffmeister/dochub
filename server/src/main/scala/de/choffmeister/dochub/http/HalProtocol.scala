package de.choffmeister.dochub.http

import play.api.libs.json._

final case class HalPage[T](items: Seq[T], totalItems: Int)
object HalPage {
  def from[T](items: Seq[T]): HalPage[T] = from(items, Some(items.size))
  def from[T](items: Seq[T], totalItems: Option[Int] = None): HalPage[T] =
    HalPage(items, totalItems.getOrElse(items.size))
  implicit def format[T: Writes]: OWrites[HalPage[T]] = Json.writes[HalPage[T]]
}

class HalProtocol[T](inner: T)(implicit innerFormat: OWrites[T]) {
  def embed[E](key: String, embedded: E)(implicit embeddedWrites: Writes[E]): JsObject = {
    val dataJson = innerFormat.writes(inner)
    embed(dataJson, key, embedded)
  }

  private def embed[E](dataJson: JsObject, key: String, embedded: E)(implicit embeddedWrites: Writes[E]): JsObject = {
    val existingEmbedded = dataJson \ "_embedded" match {
      case JsDefined(JsObject(fields)) => fields.toSeq
      case JsDefined(_)                => throw new RuntimeException("Cannot embed value because _embedded is not an object")
      case JsUndefined()               => Seq.empty
    }
    dataJson ++ JsObject(Seq("_embedded" -> JsObject(existingEmbedded :+ (key -> embeddedWrites.writes(embedded)))))
  }
}

object HalProtocol {
  implicit def toHalProtocol[T](inner: T)(implicit innerFormat: OWrites[T]): HalProtocol[T] = new HalProtocol[T](inner)

  private implicit val jsObjectWrites: OWrites[JsObject] = (o: JsObject) => o
  implicit def toHalProtocol(inner: JsObject): HalProtocol[JsObject] = new HalProtocol[JsObject](inner)
}
