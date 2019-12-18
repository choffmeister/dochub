package de.choffmeister.dochub.data.document

import java.nio.file.StandardCopyOption.{ATOMIC_MOVE, REPLACE_EXISTING}
import java.nio.file.StandardOpenOption.{APPEND, CREATE_NEW, WRITE}
import java.nio.file.{Files, Path, Paths}
import java.time._
import java.util.UUID

import akka.http.scaladsl.model.ContentType
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data._
import de.choffmeister.dochub.data.user.UserId
import org.apache.commons.codec.binary.Hex

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class DocumentData(db: ExtendedPostgresProfile#Backend#Database, basePath: Path) extends LazyLogging {
  def searchDocuments(userId: UserId, query: String)(page: (Int, Int)): Future[(Seq[DocumentDetails], Int)] = {
    val q = documentTableQuery.list(userId, _._1.name.toLowerCase like s"%${query.toLowerCase}%")(page)
    db.run(q)
  }

  def listDocuments(userId: UserId)(page: (Int, Int)): Future[(Seq[DocumentDetails], Int)] = {
    val q = documentTableQuery.list(userId, _ => LiteralColumn(1) === LiteralColumn(1))(page)
    db.run(q)
  }

  def retrieveDocument(userId: UserId, documentId: DocumentId): Future[Option[DocumentDetails]] = {
    val q = documentTableQuery.list(userId, _._1.id === documentId)((0, 1)).map(_._1.headOption)
    db.run(q)
  }

  def createDocument(
    userId: UserId,
    name: String,
    labelIds: Set[LabelId],
    blobId: BlobId,
    contentType: ContentType
  ): Future[Document] = {
    val revisionNumber = 1
    val document = Document(
      id = DocumentId.random,
      userId = userId,
      blobId = blobId,
      revisionNumber = revisionNumber,
      name = name,
      labelIds = labelIds,
      contentType = contentType
    )
    val revision =
      Revision(number = revisionNumber, documentId = document.id, blobId = blobId, contentType = contentType)
    val q = for {
      _ <- documentTableQuery += document
      _ <- revisionTableQuery += revision
    } yield document
    db.run(q.transactionally)
  }

  def updateDocument(
    userId: UserId,
    documentId: DocumentId,
    blobId: BlobId,
    contentType: ContentType
  ): Future[Document] = {
    val q = for {
      existingDocument <- documentTableQuery
        .filter(x => x.userId === userId && x.id === documentId)
        .take(1)
        .result
        .map(_.head)
      revisionNumber = existingDocument.revisionNumber + 1
      document = existingDocument.copy(
        blobId = blobId,
        revisionNumber = revisionNumber,
        contentType = contentType,
        updatedAt = Instant.now
      )
      _ <- documentTableQuery
        .filter(_.id === documentId)
        .map(x => (x.blobId, x.revisionNumber, x.contentType, x.updatedAt))
        .update((document.blobId, document.revisionNumber, document.contentType, document.updatedAt))
      _ <- revisionTableQuery += Revision(
        number = revisionNumber,
        documentId = documentId,
        blobId = blobId,
        contentType = contentType
      )
    } yield document
    db.run(q.transactionally)
  }

  def retrieveRevision(
    userId: UserId,
    documentId: DocumentId,
    revisionNumber: Int
  ): Future[Option[(Document, Revision, Blob, Source[ByteString, NotUsed])]] = {
    val q = revisionTableQuery
      .filter(x => x.documentId === documentId && x.number === revisionNumber)
      .join(documentTableQuery)
      .on(_.documentId === _.id)
      .filter(_._2.userId === userId)
      .join(blobTableQuery)
      .on(_._1.blobId === _.id)
      .take(1)
      .map(x => (x._1._2, x._1._1, x._2))
      .result
      .map(_.headOption)
    db.run(q)
      .map(_.map {
        case (document, revision, blob) =>
          val path = blobIdToPath(blob.id)
          val bytes = if (blob.size > 0) FileIO.fromPath(path).mapMaterializedValue(_ => NotUsed) else Source.empty
          (document, revision, blob, bytes)
      })
  }

  def listLabels(userId: UserId)(page: (Int, Int)): Future[(Seq[Label], Int)] = {
    val q = labelTableQuery.list(userId, _ => LiteralColumn(1) === LiteralColumn(1))(page)
    db.run(q)
  }

  def retrieveLabels(userId: UserId, labelIds: Set[LabelId]): Future[Seq[Label]] = {
    val q = labelTableQuery.list(userId, _.id.inSet(labelIds))((0, Int.MaxValue))
    db.run(q).map(_._1)
  }

  def getBlob(id: BlobId): Future[Option[(Blob, Source[ByteString, NotUsed])]] = {
    val q = blobTableQuery.filter(_.id === id).take(1).result.map(_.headOption)
    db.run(q)
      .map(_.map { file =>
        val path = blobIdToPath(file.id)
        val bytes = if (file.size > 0) FileIO.fromPath(path).mapMaterializedValue(_ => NotUsed) else Source.empty
        (file, bytes)
      })
  }

  def findBlobByHash(
    algorithm: HashAlgorithm,
    bytes: ByteString
  ): Future[Option[(Blob, Source[ByteString, NotUsed])]] = {
    val q = algorithm match {
      case HashAlgorithm.`MD5`      => blobTableQuery.filter(_.md5 === bytes).take(1).result
      case HashAlgorithm.`SHA1`     => blobTableQuery.filter(_.sha1 === bytes).take(1).result
      case HashAlgorithm.`SHA1-256` => blobTableQuery.filter(_.sha256 === bytes).take(1).result
      case HashAlgorithm.`SHA1-512` => blobTableQuery.filter(_.sha512 === bytes).take(1).result
      case HashAlgorithm.Unknown(_) => ???
    }
    db.run(q)
      .map(_.headOption)
      .map(_.map { blob =>
        val path = blobIdToPath(blob.id)
        val bytes = if (blob.size > 0) FileIO.fromPath(path).mapMaterializedValue(_ => NotUsed) else Source.empty
        (blob, bytes)
      })
  }

  def putBlob(verify: Hashes = noHashes)(implicit materializer: Materializer): Sink[ByteString, Future[Blob]] = {
    Flow[ByteString]
      .concat(Source.single(ByteString.empty))
      .toMat(
        Sink
          .lazyInitAsync(() => {
            putBlobStart()
              .map(
                id =>
                  putBlobAppend(id)
                    .mapMaterializedValue(_.flatMap(_ => putBlobFinish(id, verify)))
                    .mapMaterializedValue(_.andThen {
                      case Failure(_) => putBlobCancel(id)
                    })
              )
          })
          .mapMaterializedValue(_.flatMap(_.get))
      )(Keep.right)
  }

  def putBlobStart(): Future[UUID] = {
    val tempId = UUID.randomUUID
    val tempPath = tempIdToPath(tempId)
    Future {
      Files.createDirectories(tempPath.getParent)
      Files.write(tempPath, new Array[Byte](0), WRITE, CREATE_NEW)
      tempId
    }
  }

  def putBlobAppend(tempId: UUID): Sink[ByteString, Future[Long]] = {
    val tempPath = tempIdToPath(tempId)
    FileIO.toPath(tempPath, Set(WRITE, APPEND)).mapMaterializedValue(_.map(_ => Files.size(tempPath)))
  }

  def putBlobFinish(tempId: UUID, verify: Hashes = noHashes)(implicit materializer: Materializer): Future[Blob] = {
    def putBlobVerify(tempId: UUID, bytes: Source[ByteString, _], expected: Hashes = Map.empty): Future[Hashes] = {
      bytes.viaMat(Hash.multi(HashAlgorithm.all))(Keep.right).to(Sink.ignore).run().flatMap { hashes =>
        val unmatched = expected
          .filter { case (algo, _) => hashes.contains(algo) }
          .filter { case (algo, bytes) => hashes(algo) != bytes }
        if (unmatched.isEmpty) Future.successful(hashes)
        else
          putBlobCancel(tempId).transform { _ =>
            val expectationLines = unmatched.map {
              case (algo, bytes) =>
                val expectedHex = Hex.encodeHexString(bytes.toArray)
                val actualHex = Hex.encodeHexString(hashes(algo).toArray)
                s"Expected $algo:$expectedHex but got $algo:$actualHex"
            }.toList
            Failure(new RuntimeException("Verification failed:\n" + expectationLines.mkString("\n")))
          }
      }
    }
    val tempPath = tempIdToPath(tempId)
    putBlobVerify(tempId, FileIO.fromPath(tempPath), verify).flatMap { hashes =>
      val id = BlobId(hashes(HashAlgorithm.`SHA1-512`))
      val path = blobIdToPath(id)
      getBlob(id).flatMap {
        case Some((blob, _)) =>
          Files.deleteIfExists(tempPath)
          Future.successful(blob)
        case None =>
          val file = Blob(
            id,
            md5 = hashes(HashAlgorithm.MD5),
            sha1 = hashes(HashAlgorithm.SHA1),
            sha256 = hashes(HashAlgorithm.`SHA1-256`),
            sha512 = hashes(HashAlgorithm.`SHA1-512`),
            size = Files.size(tempPath),
            createdAt = Instant.now
          )
          Files.createDirectories(path.getParent)
          Files.move(tempPath, path, ATOMIC_MOVE, REPLACE_EXISTING)
          db.run(blobTableQuery += file).map(_ => file)
      }
    }
  }

  def putBlobCancel(tempId: UUID): Future[Done] = {
    val path = tempIdToPath(tempId)
    Future {
      Files.deleteIfExists(path)
      Done
    }
  }

  def deleteBlob(id: BlobId): Future[Done] = {
    val path = blobIdToPath(id)
    db.run(blobTableQuery.filter(_.id === id).delete)
      .flatMap(_ => Future(Files.deleteIfExists(path)))
      .map(_ => Done)
  }

  private type Hashes = Map[HashAlgorithm, ByteString]
  private val noHashes: Hashes = Map.empty[HashAlgorithm, ByteString]

  private def tempIdToPath(tempId: UUID): Path = {
    val idStr = tempId.toString.replace("-", "").toLowerCase
    basePath.resolve("_temp").resolve(idStr)
  }
  private def blobIdToPath(id: BlobId): Path = {
    val idStr = id.toString.replace("-", "").toLowerCase
    basePath.resolve(idStr.substring(0, 2)).resolve(idStr)
  }
}

object DocumentData {
  def fromConfig(config: Config, db: ExtendedPostgresProfile#Backend#Database): DocumentData = {
    val basePath = Paths.get(config.getString("storage.directory"))
    new DocumentData(db, basePath)
  }
}
