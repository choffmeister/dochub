package de.choffmeister.dochub.data

import akka.http.scaladsl.model.{ContentType, ContentTypes, Uri}
import akka.util.ByteString
import com.github.tminglei.slickpg._
import de.choffmeister.dochub.auth.{Claim, Scope}
import de.choffmeister.dochub.data.document._
import de.choffmeister.dochub.data.user._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait ExtendedPostgresProfile
    extends ExPostgresProfile
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgPlayJsonSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = ExtendedAPI

  object ExtendedAPI
      extends API
      with ArrayImplicits
      with DateTimeImplicits
      with JsonImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
    implicit val userIdColumnType = MappedColumnType.base[UserId, String](_.toString, UserId.fromString)
    implicit val apiKeyIdColumnType = MappedColumnType.base[ApiKeyId, String](_.toString, ApiKeyId.fromString)
    implicit val blobIdColumnType = MappedColumnType.base[BlobId, String](_.toString, BlobId.fromString)
    implicit val documentIdColumnType = MappedColumnType.base[DocumentId, String](_.toString, DocumentId.fromString)
    implicit val labelIdColumnType = MappedColumnType.base[LabelId, String](_.toString, LabelId.fromString)
    implicit val uriColumnType = MappedColumnType.base[Uri, String](_.toString, Uri.apply)
    implicit val contentTypeColumnType = MappedColumnType.base[ContentType, String](
      _.toString,
      str =>
        ContentType.parse(str) match {
          case Right(mt) => mt
          case Left(_)   => ContentTypes.`application/octet-stream`
        }
    )
    implicit val byteStringColumnType = MappedColumnType.base[ByteString, Array[Byte]](_.toArray, ByteString.apply)
    implicit val claimColumnType =
      MappedColumnType.base[Claim, String](_.toString, Claim.fromString)
    implicit val claimSetColumnType =
      new AdvancedArrayJdbcType[Claim](
        "text",
        s => utils.SimpleArrayUtils.fromString[Claim](Claim.fromString)(s).orNull,
        v => utils.SimpleArrayUtils.mkString[Claim](Claim.toString)(v)
      ).to[Set](_.toSet, _.toSeq)
    implicit val scopeColumnType =
      MappedColumnType.base[Scope, String](_.toString, Scope.fromString)
    implicit val scopeSetColumnType =
      new AdvancedArrayJdbcType[Scope](
        "text",
        s => utils.SimpleArrayUtils.fromString[Scope](Scope.fromString)(s).orNull,
        v => utils.SimpleArrayUtils.mkString[Scope](Scope.toString)(v)
      ).to[Set](_.toSet, _.toSeq)
    implicit val colorColumnType = MappedColumnType.base[Color, String](_.toString, Color.fromString)
    implicit val labelIdSetColumnType =
      new AdvancedArrayJdbcType[LabelId](
        "text",
        s => utils.SimpleArrayUtils.fromString[LabelId](LabelId.fromString)(s).orNull,
        v => utils.SimpleArrayUtils.mkString[LabelId](_.toString)(v)
      ).to[Set](_.toSet, _.toSeq)
  }
}

object ExtendedPostgresProfile extends ExtendedPostgresProfile
