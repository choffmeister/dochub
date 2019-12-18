package de.choffmeister.dochub.data.document

import java.time.Instant

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.utils.JsonHelpers
import play.api.libs.json.{Format, Json, OFormat}
import slick.sql.SqlProfile.ColumnOption.SqlType

private[data] class RevisionTable(tag: Tag) extends Table[Revision](tag, "revisions") {
  def number = column[Int]("number")
  def documentId = column[DocumentId]("document_id")
  def blobId = column[BlobId]("blob_id")
  def contentType = column[ContentType]("content_type")
  def note = column[String]("note")
  def createdAt = column[Instant]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def pk = primaryKey("pk__revision", (number, documentId))
  def fkDocumentId =
    foreignKey("fk__revision__document_id", documentId, documentTableQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  def fkBlobId =
    foreignKey("fk__revision__blob_id", blobId, blobTableQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  def * =
    (number, documentId, blobId, contentType, note, createdAt) <> ((Revision.apply _).tupled, Revision.unapply)
}

private[data] object revisionTableQuery extends TableQuery(new RevisionTable(_))

final case class Revision(
  number: Int,
  documentId: DocumentId,
  blobId: BlobId,
  contentType: ContentType,
  note: String = "",
  createdAt: Instant = Instant.now
)
object Revision {
  implicit val contentTypeFormat: Format[ContentType] = JsonHelpers.mappedFormat[ContentType, String](
    str =>
      ContentType.parse(str) match {
        case Right(ct) => ct
        case Left(_)   => ContentTypes.`application/octet-stream`
      },
    _.toString
  )
  implicit val format: OFormat[Revision] = Json.format[Revision]
}
