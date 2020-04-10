package de.choffmeister.dochub.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.stream.Materializer
import de.choffmeister.microserviceutils.ServiceBase
import de.choffmeister.microserviceutils.http.HttpServerBase
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.ExecutionContext

trait HttpServerRoutes extends PlayJsonSupport {
  def routes: Route
}

class HttpServer(staticContentRoutes: StaticContentRoutes, routesGroups: Set[HttpServerRoutes])(
  implicit val system: ActorSystem,
  val executionContext: ExecutionContext,
  val materializer: Materializer
) extends HttpServerBase {
  override def routes: Route = routesGroups.foldLeft[Route](reject)((a, b) => concat(a, b.routes))

  override def routesWithHealthCheckAndWrappers(service: ServiceBase): Route =
    concat(staticContentRoutes.routes, super.routesWithHealthCheckAndWrappers(service))
}
