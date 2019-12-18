package de.choffmeister.dochub

import java.util.UUID

import com.typesafe.config.ConfigFactory
import de.choffmeister.dochub.data.ExtendedPostgresProfile.api._
import de.choffmeister.dochub.data.{DataInit, ExtendedPostgresProfile}
import slick.basic.DatabaseConfig

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object DatabaseTest {
  def apply(inner: ExtendedPostgresProfile#Backend#Database => Unit) = {
    val id = UUID.randomUUID().toString.replaceAll("-", "")
    val databaseName = s"test_${id}"
    val databaseConfigRaw = ConfigFactory
      .parseString(s"""database.db.properties.databaseName = "$databaseName"""")
      .withFallback(ConfigFactory.defaultOverrides())
      .withFallback(ConfigFactory.parseResources("reference.conf"))
      .resolve()
    val databaseConfig = DatabaseConfig.forConfig[ExtendedPostgresProfile]("database", databaseConfigRaw)
    val host = databaseConfigRaw.getString("database.db.properties.serverName")
    val port = databaseConfigRaw.getInt("database.db.properties.portNumber")
    val user = databaseConfigRaw.getString("database.db.properties.user")
    val pass = databaseConfigRaw.getString("database.db.properties.password")
    val url = s"jdbc:postgresql://$host:$port/"
    val driver = "org.postgresql.Driver"

    def conn = Database.forURL(url, user, pass, driver = driver)
    try {
      using(conn)(db => Await.result(db.run(sqlu"CREATE DATABASE #$databaseName"), 10.seconds))
      using(databaseConfig.db) { db =>
        Await.result(DataInit.run(db), 10.seconds)
        inner(db)
      }
    } finally {
      using(conn)(db => Await.result(db.run(sqlu"DROP DATABASE IF EXISTS #$databaseName"), 10.seconds))
    }
  }

  private def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }
}
