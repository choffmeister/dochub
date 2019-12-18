package de.choffmeister.dochub

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

abstract class AkkaTestKit
    extends TestKit(ActorSystem(getClass.getSimpleName.replaceAll("[^a-zA-Z0-9]", "")))
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with Eventually {
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(10, Millis))
  implicit val timeout = RouteTestTimeout(15.seconds)
  implicit val executor = ExecutionContext.Implicits.global
  implicit val materializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}

object AkkaTestKit {
  def createActorSystem(): ActorSystem = {
    val id = UUID.randomUUID().toString.replaceAll("-", "")
    val actorSystemName = s"test_$id"
    ActorSystem(actorSystemName)
  }
}
