package de.choffmeister.dochub.utils

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server.{Directive1, MalformedQueryParamRejection}

object AkkaHttpHelpers {
  def paging(defaultLimit: Int = 25, maximumLimit: Int = 100): Directive1[(Int, Int)] = {
    require(defaultLimit > 0, "Default limit must be positive")
    require(maximumLimit > 0, "Maximum limit must be positive")
    require(defaultLimit <= maximumLimit, "Default limit must not be larger than maximum limit")
    parameters('from.as[Int].?, 'limit.as[Int].?).tflatMap {
      case (fromRaw, limitRaw) =>
        val from = fromRaw.getOrElse(0)
        val limit = limitRaw.getOrElse(defaultLimit)
        if (from < 0) reject(MalformedQueryParamRejection("from", "From must not be negative"))
        else if (limit < 1 || limit > maximumLimit)
          reject(MalformedQueryParamRejection("limit", s"Limit must be between 1 and $maximumLimit"))
        else provide((from, limit))
    }
  }

  def extractContentType(fileName: String): Directive1[ContentType] = {
    val resolver = ContentTypeResolver.Default
    optionalHeaderValueByType[`Content-Type`]().flatMap {
      case Some(header) => provide(header.contentType)
      case None         => provide(resolver(fileName))
    }
  }
}
