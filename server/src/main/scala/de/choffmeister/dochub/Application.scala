package de.choffmeister.dochub

import de.choffmeister.microserviceutils.ApplicationBase

object Application extends ApplicationBase {
  override def run() = {
    val service = new Service()
    service.start()
  }
}
