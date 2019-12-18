package de.choffmeister.dochub.data

import java.nio.file.{Files, Path}

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data.document._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object DataCleanup extends LazyLogging {
  private implicit val ec = ExecutionContext.Implicits.global

  def run(db: ExtendedPostgresProfile#Backend#Database, base: Path)(
    implicit materializer: Materializer
  ): Future[Done] = {
    deepPathSource(base)
      .filter(p => Files.isRegularFile(p))
      .mapAsync(1) { path =>
        if (path.getParent == base.resolve("_temp")) {
          Files.deleteIfExists(path)
          logger.info(s"Removed temporary file [$path]")
          Future.successful(1)
        } else {
          val blobId = Try(BlobId.fromString(path.getFileName.toString)).getOrElse(BlobId(ByteString.empty))
          db.run(blobTableQuery.filter(_.id === blobId).take(1).result.map(_.nonEmpty)).flatMap {
            case true =>
              db.run(revisionTableQuery.filter(_.blobId === blobId).take(1).result.map(_.nonEmpty)).flatMap {
                case true =>
                  Future.successful(0)
                case false =>
                  db.run(blobTableQuery.filter(_.id === blobId).delete).map { _ =>
                    Files.deleteIfExists(path)
                    logger.info(s"Removed orphaned file [$path]")
                    1
                  }
              }
            case false =>
              Files.deleteIfExists(path)
              logger.info(s"Removed orphaned file [$path]")
              Future.successful(1)
          }
        }
      }
      .runFold(0)(_ + _)
      .map { total =>
        logger.info(s"Removed a total of [$total] files")
        Done
      }
  }

  private def deepPathSource(base: Path): Source[Path, NotUsed] = {
    if (!Files.isDirectory(base)) Source.empty
    else
      Source
        .fromIterator(() => Files.newDirectoryStream(base).iterator().asScala)
        .flatMapConcat { path =>
          if (Files.isDirectory(path)) deepPathSource(path).concat(Source.single(path))
          else Source.single(path)
        }
        .filter(p => p.isAbsolute)
  }
}
