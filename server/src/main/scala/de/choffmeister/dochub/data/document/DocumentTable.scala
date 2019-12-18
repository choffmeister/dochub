package de.choffmeister.dochub.data.document

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data.user.{UserId, userTableQuery}
import de.choffmeister.dochub.utils.JsonHelpers
import play.api.libs.json.{Format, Json, OFormat}
import slick.lifted.CanBeQueryCondition
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.ExecutionContext

private[data] class DocumentTable(tag: Tag) extends Table[Document](tag, "documents") {
  def id = column[DocumentId]("id", O.PrimaryKey)
  def userId = column[UserId]("user_id")
  def blobId = column[BlobId]("blob_id")
  def revisionNumber = column[Int]("revision_number")
  def name = column[String]("name")
  def labelIds = column[Set[LabelId]]("label_ids")
  def contentType = column[ContentType]("content_type")
  def createdAt = column[Instant]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def updatedAt = column[Instant]("updated_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def fkUserId =
    foreignKey("fk__document__user_id", userId, userTableQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  def fkBlobId =
    foreignKey("fk__document__blob_id", blobId, blobTableQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  def idxCreatedAt = index("idx__document__created_at", createdAt)
  def * =
    (id, userId, blobId, revisionNumber, name, labelIds, contentType, createdAt, updatedAt) <> ((Document.apply _).tupled, Document.unapply)
}

private[data] object documentTableQuery extends TableQuery(new DocumentTable(_)) {
  type ListFilter = (DocumentTable, BlobTable)

  def list[T <: Rep[_]](userId: UserId, filter: ListFilter => T)(
    page: (Int, Int)
  )(implicit wt: CanBeQueryCondition[T], ec: ExecutionContext) = {
    val q = documentTableQuery
      .filter(_.userId === userId)
      .join(blobTableQuery)
      .on(_.blobId === _.id)
      .filter(filter)
      .sortBy(_._1.createdAt.desc)
    for {
      documents <- q
        .drop(page._1)
        .take(page._2)
        .result
        .map(_.map {
          case (document, blob) =>
            DocumentDetails(document, blob.size)
        })
      totalCount <- q.size.result
    } yield (documents, totalCount)
  }
}

final case class DocumentId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}
object DocumentId {
  def empty: DocumentId = fromString("00000000-0000-0000-0000-000000000000")
  def random: DocumentId = DocumentId(UUID.randomUUID)
  def fromString(value: String): DocumentId = DocumentId(UUID.fromString(value))
  implicit val format: Format[DocumentId] = Json.valueFormat[DocumentId]
}

final case class Document(
  id: DocumentId,
  userId: UserId,
  blobId: BlobId,
  revisionNumber: Int,
  name: String,
  labelIds: Set[LabelId],
  contentType: ContentType,
  createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now
)
object Document {
  implicit val contentTypeFormat: Format[ContentType] = JsonHelpers.mappedFormat[ContentType, String](
    str =>
      ContentType.parse(str) match {
        case Right(ct) => ct
        case Left(_)   => ContentTypes.`application/octet-stream`
      },
    _.toString
  )
  implicit val format: OFormat[Document] = Json.format[Document]
}

final case class DocumentDetails(
  id: DocumentId,
  userId: UserId,
  blobId: BlobId,
  revisionNumber: Int,
  name: String,
  labelIds: Set[LabelId],
  contentType: ContentType,
  createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now,
  size: Long
)
object DocumentDetails {
  def apply(document: Document, size: Long): DocumentDetails = DocumentDetails(
    id = document.id,
    userId = document.userId,
    blobId = document.blobId,
    revisionNumber = document.revisionNumber,
    name = document.name,
    labelIds = document.labelIds,
    contentType = document.contentType,
    createdAt = document.createdAt,
    updatedAt = document.updatedAt,
    size = size
  )
  implicit val contentTypeFormat: Format[ContentType] = JsonHelpers.mappedFormat[ContentType, String](
    str =>
      ContentType.parse(str) match {
        case Right(ct) => ct
        case Left(_)   => ContentTypes.`application/octet-stream`
      },
    _.toString
  )
  implicit val format: OFormat[DocumentDetails] = Json.format[DocumentDetails]
}
