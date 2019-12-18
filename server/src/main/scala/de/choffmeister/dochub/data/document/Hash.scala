package de.choffmeister.dochub.data.document

import java.security.MessageDigest

import akka.stream.scaladsl.Flow
import akka.util.ByteString
import de.choffmeister.dochub.utils.JsonHelpers
import play.api.libs.json.Format

import scala.concurrent.{ExecutionContext, Future}

sealed trait HashAlgorithm {
  def javaAlgorithm: String
  override def toString: String = HashAlgorithm.toString(this)
}
object HashAlgorithm {
  case object MD5 extends HashAlgorithm {
    override val javaAlgorithm: String = "MD5"
  }
  case object SHA1 extends HashAlgorithm {
    override val javaAlgorithm: String = "SHA1"
  }
  case object `SHA1-256` extends HashAlgorithm {
    override val javaAlgorithm: String = "SHA-256"
  }
  case object `SHA1-512` extends HashAlgorithm {
    override val javaAlgorithm: String = "SHA-512"
  }
  final case class Unknown(other: String) extends HashAlgorithm {
    override def javaAlgorithm: String = throw new RuntimeException(s"Unknown hash algorithm '$other'")
  }

  val all: Set[HashAlgorithm] = Set(MD5, SHA1, `SHA1-256`, `SHA1-512`)

  def toString(hash: HashAlgorithm): String = hash match {
    case MD5        => "md5"
    case SHA1       => "sha1"
    case `SHA1-256` => "sha1-256"
    case `SHA1-512` => "sha1-512"
    case h: Unknown => h.other
  }
  def fromString(str: String): HashAlgorithm = str match {
    case "md5"      => MD5
    case "sha1"     => SHA1
    case "sha1-256" => `SHA1-256`
    case "sha1-512" => `SHA1-512`
    case other      => Unknown(other)
  }
  implicit val format: Format[HashAlgorithm] =
    JsonHelpers.mappedFormat[HashAlgorithm, String](fromString, _.toString)
}

object Hash {
  def apply(
    algorithm: HashAlgorithm
  )(implicit executor: ExecutionContext): Flow[ByteString, ByteString, Future[ByteString]] = {
    Flow
      .lazyInitAsync(() => {
        val hasher = MessageDigest.getInstance(algorithm.javaAlgorithm)
        val flow = Flow[ByteString]
          .map { chunk =>
            hasher.update(chunk.toArray)
            chunk
          }
          .watchTermination() { case (_, done) => done.map(_ => ByteString(hasher.digest())) }
        Future.successful(flow)
      })
      .mapMaterializedValue(
        x =>
          x.flatMap {
            case Some(x) => x
            case None =>
              Future.successful {
                val hasher = MessageDigest.getInstance(algorithm.javaAlgorithm)
                ByteString(hasher.digest())
              }
          }
      )
  }

  def multi(
    algorithms: Set[HashAlgorithm]
  )(implicit executor: ExecutionContext): Flow[ByteString, ByteString, Future[Map[HashAlgorithm, ByteString]]] = {
    val zero = Flow[ByteString].mapMaterializedValue(_ => Future.successful(Map.empty[HashAlgorithm, ByteString]))
    algorithms.foldLeft(zero) {
      case (flow, algorithm) =>
        flow
          .viaMat(apply(algorithm)) { (leftF, rightF) =>
            (leftF.zip(rightF)).map {
              case (left, right) =>
                left + (algorithm -> right)
            }
          }
    }
  }
}
