package de.choffmeister.dochub.utils

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

final case class OcrmypdfError(message: String) extends RuntimeException(message) with NoStackTrace

class OcrmypdfClient(url: Uri)(implicit sys: ActorSystem, mat: Materializer, ec: ExecutionContext) {
  def apply(bytes: Source[ByteString, NotUsed], size: Long): Source[ByteString, NotUsed] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = url,
      entity = HttpEntity(ContentTypes.`application/octet-stream`, size, bytes)
    )
    Source
      .fromFutureSource(Http().singleRequest(request).flatMap {
        case response if response.status.isSuccess =>
          Future.successful(response.entity.dataBytes)
        case response =>
          response.entity.toStrict(3.seconds).flatMap { entity =>
            Future.failed(OcrmypdfError(entity.data.utf8String))
          }
      })
      .mapMaterializedValue(_ => NotUsed)
  }
}

object OcrmypdfClient {
  def fromConfig(config: Config, sys: ActorSystem, mat: Materializer, ec: ExecutionContext): OcrmypdfClient = {
    val url = Uri(config.getString("ocrmypdf.url"))
    new OcrmypdfClient(url)(sys, mat, ec)
  }
}
