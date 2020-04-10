package de.choffmeister.dochub.auth

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import de.choffmeister.dochub.data.user.UserId

class AuthConsumer {
  def authenticate: Directive1[UserId] = {
    provide(UserId.empty)
  }
}
