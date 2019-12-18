package de.choffmeister.dochub.utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import de.choffmeister.dochub.AkkaTestKit

class TarballFlowTest extends AkkaTestKit {
  "generates tarballs" in {
    val file1 = ("file1.txt", ByteString("file1"))
    val file2 = (
      "path-".padTo(40, 'x') + "/" + "path-".padTo(40, 'x') + "/" + "file2-".padTo(80, 'x') + ".txt",
      ByteString("file1")
    )
    val source = Source(List(file1, file2)).map {
      case (filename, bytes) =>
        (filename, bytes.size.toLong, Source.single(bytes))
    }
    val tarball = source.via(TarballFlow.generate).runFold(ByteString.empty)(_ ++ _).futureValue
    tarball should have size (2048)
  }
}
