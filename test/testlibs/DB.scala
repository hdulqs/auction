package testlibs

import play.api.db.{Database, Databases}
import play.api.db.evolutions._

// methods for working with test database

object DB {

  def withTestDatabase[T](block: Database => T) = {
    Databases.withDatabase(
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/auction_test2"
    ) { database => {

        implicit val db = database

//        Evolutions.cleanupEvolutions(database)
//        Evolutions.applyEvolutions(database)
        block(database)

      }
    }
  }

}
