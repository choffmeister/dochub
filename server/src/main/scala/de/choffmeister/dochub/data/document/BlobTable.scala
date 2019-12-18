package de.choffmeister.dochub.data.document

import java.time.Instant

import akka.util.ByteString
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.utils.JsonHelpers
import org.apache.commons.codec.binary.Hex
import play.api.libs.json._
import slick.sql.SqlProfile.ColumnOption.SqlType

private[data] class BlobTable(tag: Tag) extends Table[Blob](tag, "blobs") {
  def id = column[BlobId]("id", O.PrimaryKey)
  def md5 = column[ByteString]("md5")
  def sha1 = column[ByteString]("sha1")
  def sha256 = column[ByteString]("sha256")
  def sha512 = column[ByteString]("sha512")
  def size = column[Long]("size")
  def createdAt = column[Instant]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  def idxMd5 = index("idx__blob__md5", md5, unique = true)
  def idxSha1 = index("idx__blob__sha1", sha1, unique = true)
  def idxSha256 = index("idx__blob__sha256", sha256, unique = true)
  def idxSha512 = index("idx__blob__sha512", sha512, unique = true)
  def * =
    (id, md5, sha1, sha256, sha512, size, createdAt) <> ((Blob.apply _).tupled, Blob.unapply)
}

private[data] object blobTableQuery extends TableQuery(new BlobTable(_))

final case class BlobId(value: ByteString) extends AnyVal {
  override def toString: String = Hex.encodeHexString(value.toArray)
}
object BlobId {
  def fromString(value: String): BlobId = BlobId(ByteString(Hex.decodeHex(value)))
  implicit val byteStringFormat: Format[ByteString] = JsonHelpers
    .mappedFormat[ByteString, String](str => ByteString(Hex.decodeHex(str)), hash => Hex.encodeHexString(hash.toArray))
  implicit val format: Format[BlobId] = Json.valueFormat[BlobId]
}

final case class Blob(
  id: BlobId,
  md5: ByteString,
  sha1: ByteString,
  sha256: ByteString,
  sha512: ByteString,
  size: Long,
  createdAt: Instant
)
object Blob {
  implicit val byteStringFormat: Format[ByteString] = JsonHelpers
    .mappedFormat[ByteString, String](str => ByteString(Hex.decodeHex(str)), hash => Hex.encodeHexString(hash.toArray))
  implicit val format: OFormat[Blob] = Json.format[Blob]
}
