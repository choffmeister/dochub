package de.choffmeister.dochub.connectors

import akka.Done

import scala.concurrent.Future

trait Connector {
  def init(): Future[Done]
}
