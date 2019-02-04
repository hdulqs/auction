package models

import javax.inject.Inject

import scala.concurrent.Future
import play.api.db.DBApi
import anorm.{Macro, RowParser}
import anorm.SqlParser._
import anorm._
import models.TaggedIds._
import tagging._

@javax.inject.Singleton
class ParametersStore @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")
  private val saleStateParser: RowParser[SaleState] = Macro.namedParser[SaleState]

  // look up a SaleState
  def stateForSale(saleId: SaleId): Future[Option[SaleState]] = Future {
    db.withConnection { implicit c =>
      SQL"select * from sale_state_set where external_id = {saleId}"
        .on("saleId" -> saleId.toString)
        .as(saleStateParser.singleOpt)
    }
  }

  def allSaleIds(): Future[Set[SaleId]] = Future {
    db.withConnection { implicit c =>
      SQL"SELECT external_id FROM sale_state_set".as(scalar[String].*).map(_.taggedWith[SaleIdTag]).toSet
    }
  }

}
