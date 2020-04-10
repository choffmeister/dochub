package de.choffmeister.dochub.data.document

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.apache.commons.codec.binary.Hex
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext

class HashTest
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(10, Millis))

  implicit val executor = ExecutionContext.Implicits.global

  "single" in {
    def check(bytes: Source[ByteString, _], algorithm: HashAlgorithm, expected: String) = {
      val hash = bytes.viaMat(Hash(algorithm))(Keep.right).to(Sink.ignore).run().futureValue
      val hex = Hex.encodeHexString(hash.toArray)
      hex should be(expected)
    }

    check(Source.empty, HashAlgorithm.`SHA1-256`, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    check(
      Source.single(ByteString.empty),
      HashAlgorithm.`SHA1-256`,
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    )
    check(
      Source(List(ByteString("Hello"), ByteString("World"))),
      HashAlgorithm.`SHA1-256`,
      "872e4e50ce9990d8b041330c47c9ddd11bec6b503ae9386a99da8584e9bb12c4"
    )
  }

  "multi" in {
    def check(bytes: Source[ByteString, _], expected: Map[HashAlgorithm, String]) = {
      val hashes = bytes
        .viaMat(Hash.multi(Set(HashAlgorithm.`MD5`, HashAlgorithm.`SHA1`)))(Keep.right)
        .to(Sink.ignore)
        .run()
        .futureValue
      val hexHashes = hashes.mapValues(b => Hex.encodeHexString(b.toArray))
      hexHashes should be(expected)
    }

    check(
      Source.empty,
      Map(
        HashAlgorithm.`MD5` -> "d41d8cd98f00b204e9800998ecf8427e",
        HashAlgorithm.`SHA1` -> "da39a3ee5e6b4b0d3255bfef95601890afd80709"
      )
    )
    check(
      Source.single(ByteString.empty),
      Map(
        HashAlgorithm.`MD5` -> "d41d8cd98f00b204e9800998ecf8427e",
        HashAlgorithm.`SHA1` -> "da39a3ee5e6b4b0d3255bfef95601890afd80709"
      )
    )
    check(
      Source(List(ByteString("Hello"), ByteString("World"))),
      Map(
        HashAlgorithm.`MD5` -> "68e109f0f40ca72a15e05cc22786f8e6",
        HashAlgorithm.`SHA1` -> "db8ac1c259eb89d4a131b253bacfca5f319d54f2"
      )
    )
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
