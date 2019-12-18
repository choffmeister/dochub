package de.choffmeister.dochub.http

import java.nio.file.Paths

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{MediaTypes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.config.Config

class StaticContentRoutes(config: Config) {
  val cacheHeader = `Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(365L * 24L * 60L * 60L))
  val noCacheHeader = `Cache-Control`(CacheDirectives.`no-store`)
  val webDirectory = Option(System.getProperty("app.home")).map(dir => Paths.get(dir, "web"))

  def routes: Route = webDirectory match {
    case Some(dir) =>
      val staticDirectory = dir.resolve("static")
      encodeResponse {
        concat(
          pathPrefix("static") {
            respondWithDefaultHeader(cacheHeader) {
              getFromDirectory(staticDirectory.toString)
            }
          },
          respondWithDefaultHeader(noCacheHeader) {
            concat(
              getFromDirectory(dir.toString),
              (extractUnmatchedPath & headerValueByType[Accept]()) {
                case (path, acceptHeader)
                    if !path.startsWith(Uri.Path("/api")) && acceptHeader.mediaRanges
                      .exists(_.matches(MediaTypes.`text/html`)) =>
                  getFromFile(dir.resolve("index.html").toFile)
                case _ =>
                  reject
              },
              pathEndOrSingleSlash(getFromFile(dir.resolve("index.html").toFile))
            )
          }
        )
      }
    case _ =>
      reject
  }
}
