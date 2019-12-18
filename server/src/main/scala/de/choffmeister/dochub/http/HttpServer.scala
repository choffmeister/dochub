package de.choffmeister.dochub.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{Directive0, Route}
import akka.stream.Materializer
import de.choffmeister.dochub.auth.AuthProvider
import de.choffmeister.microserviceutils.ServiceBase
import de.choffmeister.microserviceutils.http.HttpServerBase
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.ExecutionContext

trait RoutesGroup extends PlayJsonSupport {
  def routes: Route
}

class HttpServer(authProvider: AuthProvider, staticContentRoutes: StaticContentRoutes, routesGroups: Set[RoutesGroup])(
  implicit val system: ActorSystem,
  val executionContext: ExecutionContext,
  val materializer: Materializer
) extends HttpServerBase {
  override def wrappers: List[Directive0] = List(logRequestResponse(this.getClass), sealRejections, sealExceptions)

  override def routesWithHealthCheckAndWrappers(service: ServiceBase): Route =
    concat(staticContentRoutes.routes, super.routesWithHealthCheckAndWrappers(service))

  override def routes: Route =
    concat(noCacheHeaders {
      concat(
        pathPrefix("api" / "auth")(authProvider.routes),
        routesGroups.foldLeft[Route](reject)((a, b) => concat(a, b.routes))
      )
    })
}
