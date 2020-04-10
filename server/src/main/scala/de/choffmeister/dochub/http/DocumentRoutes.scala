package de.choffmeister.dochub.http

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.choffmeister.dochub.auth.AuthConsumer
import de.choffmeister.dochub.data.document._
import de.choffmeister.dochub.http.HalProtocol._
import de.choffmeister.dochub.utils.AkkaHttpHelpers._
import de.choffmeister.dochub.utils.OcrmypdfClient

import scala.concurrent.ExecutionContext

class DocumentRoutes(authConsumer: AuthConsumer, documentData: DocumentData, ocrmypdfClient: OcrmypdfClient)(
  implicit system: ActorSystem,
  executionContext: ExecutionContext,
  materializer: Materializer
) extends HttpServerRoutes {
  def searchDocumentsRoute: Route =
    (path("api" / "documents" / "search") & paging() & get) { page =>
      parameter("query") { query =>
        authConsumer.authenticate { userId =>
          val future = for {
            (documents, totalCount) <- documentData.searchDocuments(userId, query)(page)
            labelIds = documents.foldLeft(Set.empty[LabelId])(_ ++ _.labelIds)
            labels <- documentData.retrieveLabels(userId, labelIds)
          } yield (documents, totalCount, labels)
          onSuccess(future) {
            case (documents, totalCount, labels) =>
              val page = HalPage(documents, totalCount)
                .embed("labels", labels.map(l => l.id.toString -> l).toMap)
              complete(page)
          }
        }
      }
    }

  def listDocumentsRoute: Route =
    (path("api" / "documents") & paging() & get) { page =>
      authConsumer.authenticate { userId =>
        val future = for {
          (documents, totalCount) <- documentData.listDocuments(userId)(page)
          labelIds = documents.foldLeft(Set.empty[LabelId])(_ ++ _.labelIds)
          labels <- documentData.retrieveLabels(userId, labelIds)
        } yield (documents, totalCount, labels)
        onSuccess(future) {
          case (documents, totalCount, labels) =>
            val page = HalPage(documents, totalCount)
              .embed("labels", labels.map(l => l.id.toString -> l).toMap)
            complete(page)
        }
      }
    }

  def retrieveDocumentRoute: Route =
    (path("api" / "documents" / JavaUUID.map(DocumentId.apply)) & get) { documentId =>
      authConsumer.authenticate { userId =>
        val future = for {
          document <- documentData.retrieveDocument(userId, documentId)
          labelIds = document.map(_.labelIds).getOrElse(Set.empty)
          labels <- documentData.retrieveLabels(userId, labelIds)
        } yield (document, labels)
        onSuccess(future) {
          case (Some(document), labels) => complete(document.embed("labels", labels.map(l => l.id.toString -> l).toMap))
          case (None, _)                => reject
        }
      }
    }

  def createDocumentRoute: Route =
    (path("api" / "documents") & post) {
      parameter("name") { name =>
        authConsumer.authenticate { userId =>
          (extractRequestEntity & extractContentType(name)) {
            case (entity, contentType) =>
              val future = for {
                blob <- entity.dataBytes.runWith(documentData.putBlob())
                document <- documentData.createDocument(userId, name, Set.empty, blob.id, contentType)
              } yield document
              onSuccess(future) { document =>
                complete(document.embed("labels", Map.empty[String, LabelId]))
              }
          }
        }
      }
    }

  def updateDocumentRoute: Route =
    (path("api" / "documents" / JavaUUID.map(DocumentId.apply)) & post) { documentId =>
      parameter("name") { name =>
        authConsumer.authenticate { userId =>
          (extractRequestEntity & extractContentType(name)) {
            case (entity, contentType) =>
              val future = for {
                blob <- entity.dataBytes.runWith(documentData.putBlob())
                document <- documentData.updateDocument(userId, documentId, blob.id, contentType)
                labels <- documentData.retrieveLabels(userId, document.labelIds)
              } yield (document, labels)
              onSuccess(future) {
                case (document, labels) =>
                  complete(document.embed("labels", labels.map(l => l.id.toString -> l).toMap))
              }
          }
        }
      }
    }

  def downloadDocumentRoute: Route =
    (path("api" / "documents" / JavaUUID.map(DocumentId.apply) / IntNumber / "download") & get) {
      case (documentId, revisionNumber) =>
        authConsumer.authenticate { userId =>
          onSuccess(documentData.retrieveRevision(userId, documentId, revisionNumber)) {
            case Some((document, revision, blob, bytes)) =>
              val header = `Content-Disposition`(ContentDispositionTypes.inline, Map("filename" -> document.name))
              respondWithHeader(header) {
                complete(HttpEntity(revision.contentType, blob.size, bytes))
              }
            case None =>
              reject
          }
        }
    }

  def ocrDocumentRoute: Route =
    (path("api" / "documents" / JavaUUID.map(DocumentId.apply) / "ocr") & post) { documentId =>
      authConsumer.authenticate { userId =>
        onSuccess(documentData.retrieveDocument(userId, documentId)) {
          case Some(document) =>
            onSuccess(documentData.retrieveRevision(document.userId, document.id, document.revisionNumber)) {
              case Some((document, _, blob, bytes)) =>
                val future = ocrmypdfClient(bytes, blob.size).runWith(documentData.putBlob())
                onSuccess(future) { nextBlob =>
                  onSuccess(
                    documentData
                      .updateDocument(
                        document.userId,
                        document.id,
                        nextBlob.id,
                        ContentType(MediaTypes.`application/pdf`)
                      )
                  ) { _ =>
                    complete(Done)
                  }
                }
              case None =>
                reject
            }
          case None =>
            reject
        }
      }
    }

  def routes: Route =
    concat(
      searchDocumentsRoute,
      listDocumentsRoute,
      retrieveDocumentRoute,
      createDocumentRoute,
      updateDocumentRoute,
      downloadDocumentRoute,
      ocrDocumentRoute
    )
}
