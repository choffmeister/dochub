package de.choffmeister.dochub.data.user

import de.choffmeister.dochub.auth.Claim
import de.choffmeister.dochub.{AkkaTestKit, DatabaseTest}

class UserDataTest extends AkkaTestKit {
  "reuses existing user" in DatabaseTest { db =>
    val userData = new UserData(db)

    val user1a = userData.updateUser("github:1", None, "foo1a", Set.empty).futureValue
    val user1b = userData.updateUser("github:1", None, "foo1b", Set.empty).futureValue
    val user2 = userData.updateUser("github:2", None, "foo2", Set.empty).futureValue

    user1a.id should be(user1b.id)
    user1a.id should not be (user2.id)
  }

  "stores claims" in DatabaseTest { db =>
    val userData = new UserData(db)

    val claims = Set[Claim](Claim.User("foo"), Claim.Group("bar"), Claim.Unknown("foo:bar"))
    val user1 = userData.updateUser("github:3", None, "foo3", claims).futureValue
    val user2 = userData.findUserById(user1.id).futureValue.get
    user2.claims.toSet should be(claims)
  }
}
