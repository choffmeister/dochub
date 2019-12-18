package de.choffmeister.dochub

import de.choffmeister.dochub.data.ExtendedPostgresProfile
import de.choffmeister.microserviceutils.components.BaseComponents
import slick.basic.DatabaseConfig

trait DatabaseComponents { this: BaseComponents =>
  def database: ExtendedPostgresProfile#Backend#Database
}

trait DefaultDatabaseComponents extends DatabaseComponents { this: BaseComponents =>
  private val databaseConfig = DatabaseConfig.forConfig[ExtendedPostgresProfile]("database", config)
  lazy val database = databaseConfig.db
}
