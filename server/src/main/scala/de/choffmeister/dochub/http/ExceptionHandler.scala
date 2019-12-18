package de.choffmeister.dochub.http

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{IllegalRequestException, StatusCode}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, ExceptionHandler => BaseExceptionHandler}
import akka.http.scaladsl.settings.RoutingSettings
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsObject, JsString}

import scala.util.control.{NoStackTrace, NonFatal}

abstract class BusinessError(val status: StatusCode, val code: String, val summary: String, val detail: Option[String])
    extends RuntimeException(summary + detail.map(": " + _).getOrElse(""))
    with NoStackTrace

object ExceptionHandler extends PlayJsonSupport {
  def apply(settings: RoutingSettings): BaseExceptionHandler = new BaseExceptionHandler {
    override def apply(err: Throwable): Route = {
      val (status, code, summary, details) = if (errorPF.isDefinedAt(err)) {
        errorPF(err)
      } else {
        (InternalServerError, "internalServerError", Option(err.getMessage).map(_.trim).filter(_.nonEmpty), None)
      }

      ctx => {
        val messageTemplate = "Request error [{}]. Completing with HTTP [{}] response. Details are [{}]"
        val fallbackSummary = err.getClass.getSimpleName
        if (status.intValue < 500) {
          ctx.log.warning(messageTemplate, summary.getOrElse(fallbackSummary), status.intValue, details.getOrElse(""))
        } else {
          ctx.log
            .error(err, messageTemplate, summary.getOrElse(fallbackSummary), status.intValue, details.getOrElse(""))
        }
        ctx.complete(
          (status, format(err, code, summary.getOrElse(fallbackSummary), details, settings.verboseErrorMessages))
        )
      }
    }

    override def isDefinedAt(err: Throwable): Boolean = NonFatal.unapply(err).isDefined

    override def withFallback(that: BaseExceptionHandler): BaseExceptionHandler = this

    override def seal(settings: RoutingSettings): BaseExceptionHandler = this
  }

  def sealRoute(route: Route): Route =
    extractRequestContext(ctx => handleExceptions(ExceptionHandler(ctx.settings))(route))

  private def errorPF: PartialFunction[Throwable, (StatusCode, String, Option[String], Option[String])] = {
    case err: BusinessError =>
      (err.status, err.code, Some(err.summary), err.detail)
    case err: IllegalRequestException =>
      (err.status, "illegalRequest", Some(err.info.summary), Option(err.info.summary).filter(_.nonEmpty))
  }

  private def format(
    err: Throwable,
    code: String,
    summary: String,
    detail: Option[String],
    verbose: Boolean
  ): JsObject = {
    if (verbose) {
      val stacktraceStr = Option(err.getStackTrace).getOrElse(Array.empty[String]).mkString("\n")
      JsObject(
        Seq(
          "code" -> JsString(code),
          "message" -> JsString(summary),
          "detail" -> JsString(detail.getOrElse("")),
          "stacktrace" -> JsString(stacktraceStr)
        )
      )
    } else {
      JsObject(
        Seq("code" -> JsString(code), "message" -> JsString(summary), "detail" -> JsString(detail.getOrElse("")))
      )
    }
  }
}
