package de.choffmeister.dochub.data

import akka.Done
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data.document._
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}

object DataInit {
  def run(db: ExtendedPostgresProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Done] = {
    val q = for {
      _ <- MTable.getTables.flatMap { tables =>
        DBIO.seq(
          Seq(blobTableQuery, documentTableQuery, revisionTableQuery, labelTableQuery)
            .map { table =>
              if (!tables.exists(t => t.name.name == table.baseTableRow.tableName)) Some(table.schema.create)
              else None
            }
            .collect { case Some(dbio) => dbio }: _*
        )
      }
    } yield Done
    db.run(q)
  }
}
