package de.choffmeister.dochub.utils

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

object AkkaStreamHelpers {
  def bytes(source: Source[ByteString, _], maxSize: Int = 1024 * 1024)(
    implicit materializer: Materializer
  ): Future[ByteString] = {
    source.runWith(Sink.fold(ByteString.empty) { (acc, chunk) =>
      if (acc.size + chunk.size <= maxSize) acc ++ chunk
      else throw new RuntimeException(s"Byte stream exceeded $maxSize bytes")
    })
  }

  def paginate[T](
    fn: => (Tuple2[Int, Int] => Future[(Seq[T], Int)])
  )(implicit ec: ExecutionContext): Source[T, NotUsed] = {
    val chunkSize = 12
    Source
      .unfoldAsync[Option[Int], Seq[T]](Some(0)) {
        case Some(page) =>
          fn((page * chunkSize, chunkSize)).map { chunk =>
            val nextPage = if (chunk._1.size == chunkSize) Some(page + 1) else None
            Some((nextPage, chunk._1))
          }
        case None =>
          Future.successful(None)
      }
      .mapConcat(_.toVector)
  }
}
