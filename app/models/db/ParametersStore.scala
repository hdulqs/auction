package models.db

import anorm.SqlParser._
import anorm._
import javax.inject.Inject
import models.MaxBidRule.EarlyHighBidderPriority
import models.NextIncrementRule.SnapToPresetIncrements
import models.TaggedIds._
import models._
import tagging._
import play.api.db.{DBApi, Database}

import scala.concurrent.Future

@javax.inject.Singleton
class ParametersStore @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext, db: Database) {

  // look up a SaleState
  def stateForSale(saleId: SaleId): Future[Option[SaleState]] = Future {
    db.withConnection { implicit c =>
      SQL("SELECT * FROM sale_state_set WHERE external_id = {saleId}")
        .on("saleId" -> saleId.toString)
        .as(saleParser.singleOpt)
    }
  }

  def allSaleIds(): Future[Set[SaleId]] = Future {
    println(s"allSaleIds: ${db.url}")
    db.withConnection { implicit c =>
      SQL"SELECT external_id FROM sale_state_set".as(scalar[String].*).map(_.taggedWith[SaleIdTag]).toSet
    }
  }

  def lotIdsForSaleId(saleId: SaleId): Future[Set[LotId]] = Future {
    db.withConnection { implicit c =>
      SQL("""
          SELECT external_id FROM lot_parameters_set l JOIN sale_state_set s ON
          l.sale_state_set_id = s.id WHERE s.external_id = {saleId}
          """)
        .on("saleId" -> saleId.toString)
        .as(scalar[String].*)
        .map(_.taggedWith[LotIdTag]).toSet
    }
  }

  def parametersForLot(lotId: LotId): Future[Option[LotParameters]] = Future {
    db.withConnection { implicit c =>
      SQL("select * from lot_parameters_set where external_id = {lotId}")
        .on("lotId" -> lotId.toString)
        .as(lotParser.singleOpt)
    }
  }

  def createSaleState(saleState: SaleState): Future[Option[Long]] = Future {
    println(s"createSaleState: ${db.url}")
    db.withConnection { implicit c =>
      SQL("INSERT INTO sale_state_set (external_id) VALUES ({saleId})")
        .on("saleId" -> saleState.saleId.toString).executeInsert()
    }
  }

  def persistLotParameters(lotParameters: LotParameters): Future[Option[Long]] = Future {
    db.withConnection { implicit c =>
      SQL("INSERT INTO lot_parameters_set (external_id, sale_state_set_id) VALUES ({lotId}, {saleId})")
        .on("lotId" -> lotParameters.lotId.toString, "saleId" -> lotParameters.saleId.toString).executeInsert()
    }
  }

  def bannedUserIdsInSale(saleId: SaleId): Future[Set[UserId]] = Future {
    db.withConnection { implicit c =>
      SQL("SELECT user_id FROM users_banned_in_sales WHERE sale_state_set_id = {saleId}")
        .on("saleId" -> saleId.toString)
        .as(scalar[String].*)
        .map(_.taggedWith[UserIdTag]).toSet
    }
  }

  // end of public methods and values

//  private val db = dbapi.database("default")

  private val saleParser: RowParser[SaleState] = Macro.namedParser[SaleState]

  private val lotParser =
    str("external_id") ~
    str("sale_state_set_id") ~
    long("initial_starting_price_cents") ~
    long("initial_reserve_cents") ~
    str("increment_policy_id") ~
    get[Option[Long]]("estimate_value_cents") ~
    get[Option[Long]]("low_estimate_cents") ~
    get[Option[Long]]("high_estimate_cents") ~
    str("initial_max_bid_rule") ~
    str("initial_next_increment_rule") map {
      case lotId ~
        saleId ~
        initialStartingPriceCents ~
        initialReserveCents ~
        incrementPolicyId ~
        estimateValueCents ~
        lowEstimateCents ~
        highEstimateCents ~
        initialMaxBidRule ~
        initialNextIncrementRule =>
        LotParameters(
          lotId.taggedWith[LotIdTag],
          saleId.taggedWith[SaleIdTag],
          initialStartingPriceCents,
          columnToReserve(initialReserveCents),
          incrementPolicyId.taggedWith[IncrementPolicyIdTag],
          columnToEstimate(estimateValueCents, lowEstimateCents, highEstimateCents),
          MaxBidRule.fromString(initialMaxBidRule).getOrElse(EarlyHighBidderPriority),
          NextIncrementRule.fromString(initialNextIncrementRule).getOrElse(SnapToPresetIncrements)
        )
    }

  // handle conversion of tagged types such as SaleId, LotId
  private implicit def columnToTaggedId[A]: Column[String @@ A] = Column { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case s: String => Right(s.taggedWith[A])
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to tagged type for column $qualified"))
    }
  }

  private def columnToEstimate(estimateValueCents: Option[Long], lowEstimateCents: Option[Long], highEstimateCents: Option[Long]): Estimate = {
    (estimateValueCents, lowEstimateCents, highEstimateCents) match {
      case (None, None, None) => NoEstimate
      case (Some(value), None, None) => EstimateValue(value)
      case (None, Some(low), Some(high)) => EstimateRange(low, high)
      case _ =>
        throw new IllegalArgumentException(
          "estimate should be well-defined with estimateCents or both lowEstimateCents and highEstimateCents fields"
        )
    }
  }

  private def columnToReserve(value: Long): Option[Reserve] = value match {
    case x if x < 0 => Some(UnknownReserve)
    case x if x == 0 => None
    case x => Some(KnownReserve(x))
  }
}
