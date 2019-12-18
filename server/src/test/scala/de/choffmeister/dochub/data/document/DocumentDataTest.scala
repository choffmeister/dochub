package de.choffmeister.dochub.data.document

import java.nio.file.Files

import akka.NotUsed
import akka.http.scaladsl.model.ContentTypes
import akka.stream.scaladsl.Source
import akka.util.ByteString
import de.choffmeister.dochub.data.ExtendedPostgresProfile
import de.choffmeister.dochub.data.document.HashAlgorithm.{MD5, SHA1, Unknown}
import de.choffmeister.dochub.data.user.UserData
import de.choffmeister.dochub.{AkkaTestKit, DatabaseTest}
import org.apache.commons.codec.binary.Hex

class DocumentDataTest extends AkkaTestKit {
  def prepare(db: ExtendedPostgresProfile#Backend#Database) = {
    val basePath = Files.createTempDirectory("dochub")
    val userData = new UserData(db)
    val documentData = new DocumentData(db, basePath)
    val user = userData.updateUser("github:u1", None, "u1", Set.empty).futureValue
    (basePath, userData, documentData, user)
  }

  "creates/updates documents" in DatabaseTest { db =>
    val (_, _, documentData, user) = prepare(db)
    val blob = Source.empty.runWith(documentData.putBlob()).futureValue
    val doc1a =
      documentData
        .createDocument(user.id, "test", Set.empty, blob.id, ContentTypes.`application/octet-stream`)
        .futureValue
    val doc1b =
      documentData.updateDocument(user.id, doc1a.id, blob.id, ContentTypes.`application/octet-stream`).futureValue
    val doc2a =
      documentData
        .createDocument(user.id, "test", Set.empty, blob.id, ContentTypes.`application/octet-stream`)
        .futureValue

    val docs = documentData.listDocuments(user.id)((0, Int.MaxValue)).futureValue._1
    docs.map(d => (d.id, d.revisionNumber)).toSet should be(Set((doc1a.id, 2), (doc2a.id, 1)))
  }

  "writes/reads blobs" in DatabaseTest { db =>
    val (_, _, documentData, _) = prepare(db)

    def test(key: String, bytes: ByteString, source: Source[ByteString, NotUsed]) = {
      val blob1 = source.runWith(documentData.putBlob()).futureValue
      blob1.size should be(bytes.size)
      val blob2 = documentData.getBlob(blob1.id).futureValue.get
      blob1 should be(blob2._1)
      blob2._2.runFold(ByteString.empty)(_ ++ _).futureValue should be(bytes)
    }

    test("f1", ByteString.empty, Source.empty)
    test("f2", ByteString.empty, Source.single(ByteString.empty))
    test("f3", ByteString("part1"), Source(List(ByteString("part1"))))
    test("f4", ByteString("part1part2"), Source(List(ByteString("part1"), ByteString("part2"))))
  }

  "writes blobs chunked " in DatabaseTest { db =>
    val (_, _, documentData, _) = prepare(db)

    def test(key: String, chunks: (ByteString, Source[ByteString, NotUsed])*) = {
      val id = documentData.putBlobStart().futureValue
      chunks.foreach(chunk => chunk._2.runWith(documentData.putBlobAppend(id)).futureValue)
      val blob1 = documentData.putBlobFinish(id).futureValue
      blob1.size should be(chunks.map(_._1.size).sum)
      val blob2 = documentData.getBlob(blob1.id).futureValue.get
      blob1 should be(blob2._1)
      blob2._2.runFold(ByteString.empty)(_ ++ _).futureValue should be(chunks.foldLeft(ByteString.empty)(_ ++ _._1))
    }

    test("f1", (ByteString.empty, Source.empty))
    test("f2", (ByteString.empty, Source.single(ByteString.empty)))
    test("f3", (ByteString("part1"), Source(List(ByteString("part1")))))
    test(
      "f4",
      (ByteString("part1part2"), Source(List(ByteString("part1"), ByteString("part2")))),
      (ByteString("part3"), Source(List(ByteString("part3"))))
    )
  }

  "hashes blobs" in DatabaseTest { db =>
    val (_, _, documentData, _) = prepare(db)

    val bytes1 = Source.empty[ByteString]
    val blob1 = bytes1.runWith(documentData.putBlob()).futureValue
    blob1.md5 should be(ByteString(Hex.decodeHex("d41d8cd98f00b204e9800998ecf8427e")))
    blob1.sha1 should be(ByteString(Hex.decodeHex("da39a3ee5e6b4b0d3255bfef95601890afd80709")))
    blob1.sha256 should be(
      ByteString(Hex.decodeHex("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
    )
    blob1.sha512 should be(
      ByteString(
        Hex.decodeHex(
          "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"
        )
      )
    )

    val bytes2 = Source(List(ByteString("part1"), ByteString("part2")))
    val blob2 = bytes2.runWith(documentData.putBlob()).futureValue
    blob2.md5 should be(ByteString(Hex.decodeHex("33a9ed386944493dbabcda6111c4769b")))
    blob2.sha1 should be(ByteString(Hex.decodeHex("e62f1f66616374a22bd3c87210c659b9c8cee938")))
    blob2.sha256 should be(
      ByteString(Hex.decodeHex("4986fc0feee88f7e3d1441ba8fe651b1626115c2244e45ee1ed1fe850e9ef428"))
    )
    blob2.sha512 should be(
      ByteString(
        Hex.decodeHex(
          "db2b40ed239592cfbe1cfcbfae120d4ded7783a58d1dc5d575b1d1f05056ba4a153dbf60249df87dd7030f1bc04b0acc0558c98db375a463b41f3ed9d2dc1135"
        )
      )
    )
  }

  "cancels blobs if hash verification fails" in DatabaseTest { db =>
    val (_, _, documentData, _) = prepare(db)

    val bytes1 = Source.single(ByteString("test1"))
    val bytes2 = Source.single(ByteString("test2"))
    val expected = Map[HashAlgorithm, ByteString](
      MD5 -> ByteString(Hex.decodeHex("5a105e8b9d40e1329780d62ea2265d8a")),
      SHA1 -> ByteString(Hex.decodeHex("b444ac06613fc8d63795be9ad0beaf55011936ac")),
      Unknown("other") -> ByteString.empty
    )

    bytes1.runWith(documentData.putBlob(expected)).futureValue

    the[RuntimeException] thrownBy bytes2
      .runWith(documentData.putBlob(expected))
      .futureValue should have message ("The future returned an exception of type: java.lang.RuntimeException, with message: " + List(
      "Verification failed:",
      "Expected md5:5a105e8b9d40e1329780d62ea2265d8a but got md5:ad0234829205b9033196ba818f7a872b",
      "Expected sha1:b444ac06613fc8d63795be9ad0beaf55011936ac but got sha1:109f4b3c50d7b0df729d299bc6f8e9ef9066971f"
    ).mkString("\n") + ".")

    val tempId = documentData.putBlobStart().futureValue
    bytes2.runWith(documentData.putBlobAppend(tempId)).futureValue
    a[RuntimeException] should be thrownBy documentData.putBlobFinish(tempId, expected).futureValue

//    TODO check
  }
}
