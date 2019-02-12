package models

import models.TaggedIds.{IncrementPolicyId, LotId, SaleId}

/** The types of Estimate that we can work with. */
sealed trait Estimate

/** An unspecified estimate. */
case object NoEstimate extends Estimate

/** An estimate at a specific value. */
case class EstimateValue(amountCents: Long) extends Estimate

/** An estimate range between a low and high value. */
case class EstimateRange(lowAmountCents: Long, highAmountCents: Long)
  extends Estimate

/**
  * Represents the existence of a lot in Causality, as well as its initial
  * configuration. The canonical record of a lot is in Gravity, and this is
  * the representation of its configuration from the perspective of the
  * auction engine.
  *
  * Lots are uniquely identified by a lotId, which should be supplied
  * externally. LotParameters mutable and immutable data. Anything that
  * factors in to an `AuctionCalculator`'s logic should be considered
  * immutable. This allows for the auction history to be recreated at any past
  * point in time. This data can effectively be edited by posting events. This
  * allows edits to LotParameters to be subject to validation based on past
  * events, and for the edits to be synchronized in the same way as bids.
  *
  * @param lotId (immutable) the external ID.
  * @param saleId (immutable) the external ID of the sale.
  * @param initialStartingPriceCents (immutable) the initial asking price.
  * @param initialReserveCents (immutable) the initial reserve price.
  * @param initialIncrementPolicyId (immutable) ID of increment policy.
  * @param initialEstimate (immutable) estimate for the lot.
  * @param initialMaxBidRule (immutable) the rule for early high bids.
  * @param initialNextIncrementRule (immutable) the rule for adding
  *                                 increments to off-increment bids.
  */
case class LotParameters(
                          lotId: LotId,
                          saleId: SaleId,
                          initialStartingPriceCents: Long,
                          initialReserveCents: Option[Reserve],
                          initialIncrementPolicyId: IncrementPolicyId,
                          initialEstimate: Estimate,
                          initialMaxBidRule: MaxBidRule,
                          initialNextIncrementRule: NextIncrementRule
                        ) extends {
  require(lotId.nonEmpty, "LotParameters.lotId must not be empty")
  require(saleId.nonEmpty, "LotParameters.saleId must not be empty")
  require(
    initialStartingPriceCents >= 0,
    "LotParameters.initialStartingPriceCents must be >= 0"
  )
  require(
    initialIncrementPolicyId.nonEmpty,
    "LotParameters.initialIncrementPolicyId must not be empty"
  )
}

/** The types of reserve we can work with. */
sealed trait Reserve

/** A reserve that is known to us. */
case class KnownReserve(amountCents: Long) extends Reserve {
  require(amountCents > 0, s"KnownReserve.amountCents must be > 0")
}

/**
  * A reserve that has not been revealed to us. Initially, the lot will be
  * assumed to not be meeting reserve, and the
  * [[lotevents.UnknownReserveMet]] event must be
  * explicitly published to change the reserve status.
  */
case object UnknownReserve extends Reserve

/**
  * Controls whether early high bidders get the increment of a later
  * underbidder, or if they get bumped up to the next increment. See
  * https://github.com/artsy/auctions/issues/137
  */
sealed trait MaxBidRule { def asString: String }

object MaxBidRule {
  case object EarlyHighBidderPriority extends MaxBidRule {
    val asString = "EarlyHighBidderPriority"
  }
  case object NoEarlyHighBidderPriority extends MaxBidRule {
    val asString = "NoEarlyHighBidderPriority"
  }

  def fromString(str: String): Option[MaxBidRule] =
    Seq(EarlyHighBidderPriority, NoEarlyHighBidderPriority).find(
      _.asString == str
    )
}

/**
  * Controls how to add increments. For live sales, the increments are
  * considered pre-set, and off-increment values should not cause further
  * bidding to proceed off-increment. However, for sales with open bidding
  * (eBay-style), increments should add to the previous selling price.
  */
sealed trait NextIncrementRule { def asString: String }

object NextIncrementRule {
  case object AddToPastValue extends NextIncrementRule {
    val asString = "AddToPastValue"
  }
  case object SnapToPresetIncrements extends NextIncrementRule {
    val asString = "SnapToPresetIncrements"
  }

  def fromString(str: String): Option[NextIncrementRule] =
    Seq(AddToPastValue, SnapToPresetIncrements).find(_.asString == str)
}