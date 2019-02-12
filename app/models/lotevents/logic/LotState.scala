package models.lotevents.logic

import models.TaggedIds._
import models.{Bidder, Estimate, Reserve, TemporaryIncrementStrategy}
import models.lotevents.{BiddingStatus, ReserveStatus, SoldStatus}

object LotState {

  sealed trait DerivedLotState {
    def publicForm: PublicDerivedLotState
    def externalOperatorForm: ExternalOperatorDerivedLotState
  }

  /**
    * Bundled up derived lot state, including privileged data.
    *
    * @param biddingStatus current step in the progression of bidding stages.
    * @param soldStatus whether the lot is still for sale, sold, or passing.
    * @param isOpen (deprecated) whether auction is open for bidding now.
    * @param isComplete (deprecated) whether auction has closed.
    * @param sellingPriceCents the amount of the highest bid.
    * @param askingPriceCents the minimum next bid amount.
    * @param winningBidEventId the event ID of the highest bid.
    * @param winningBidder the winning bidder.
    * @param sellingToBidder the bidder that need not make any additional bids
    *                        for the item to sell to them (reserve met, etc.)
    * @param bidCount the number of bids placed.
    * @param reserveStatus whether a lot with reserve is selling.
    * @param reserve (privileged) the exact reserve price.
    * @param maxBidAmountCents (privileged) the actual amount of the winning max
    *                          bid (as opposed to the selling price).
    * @param lastLiveOperatorEventId event ID of the latest operator action.
    * @param onlineBidCount the number of apparent bids online (after conversion
    *                       of second-price max bids to first-price proxy bids)
    * @param floorBidCount the number of bids on the floor.
    */
  case class PrivilegedDerivedLotState(
                                        askingPriceCents: Long,
                                        bidCount: Int,
                                        biddingStatus: BiddingStatus,
                                        canArtsyDoubleBid: Boolean,
                                        currentIncrementCents: Long,
                                        estimate: Estimate,
                                        floorAskingPriceCents: Option[Long] = None,
                                        floorBidCount: Option[Int] = None,
                                        floorIsComplete: Option[Boolean] = None, // TODO deprecated
                                        floorIsOpen: Option[Boolean] = None, // TODO deprecated
                                        floorReserveStatus: Option[ReserveStatus] = None,
                                        floorSellingPriceCents: Option[Long] = None,
                                        floorWinningBidder: Option[Bidder] = None,
                                        floorWinningBidEventId: Option[EventId] = None,
                                        footingData: FootingData,
                                        gmvAmountCents: Option[Long],
                                        highestArtsyFloorPriceCents: Option[Long],
                                        highestOnlinePriceCents: Option[Long],
                                        incrementPolicyId: IncrementPolicyId,
                                        isComplete: Boolean, // TODO deprecated
                                        isOpen: Boolean, // TODO deprecated
                                        lastLiveOperatorEventId: Option[EventId],
                                        maxBidAmountCents: Option[Long],
                                        onBlockSellingPriceCents: Option[Long],
                                        onlineBidCount: Option[Int] = None,
                                        reserve: Option[Reserve],
                                        reserveStatus: ReserveStatus,
                                        sellingPriceCents: Long,
                                        sellingToBidder: Option[Bidder],
                                        soldStatus: SoldStatus,
                                        temporaryIncrements: Option[TemporaryIncrementStrategy],
                                        winningBidder: Option[Bidder],
                                        winningBidEventId: Option[EventId]
                                      ) extends DerivedLotState {

    val externalOperatorForm = ExternalOperatorDerivedLotState(
      askingPriceCents = askingPriceCents,
      bidCount = bidCount,
      biddingStatus = biddingStatus,
      canArtsyDoubleBid = canArtsyDoubleBid,
      estimate = estimate,
      floorAskingPriceCents = floorAskingPriceCents,
      floorBidCount = floorBidCount,
      floorIsComplete = floorIsComplete, // TODO deprecated
      floorIsOpen = floorIsOpen, // TODO deprecated
      floorReserveStatus = floorReserveStatus,
      floorSellingPriceCents = floorSellingPriceCents,
      floorWinningBidder = floorWinningBidder,
      floorWinningBidEventId = floorWinningBidEventId,
      footingData = footingData,
      incrementPolicyId = incrementPolicyId,
      isComplete = isComplete, // TODO deprecated
      isOpen = isOpen, // TODO deprecated
      lastLiveOperatorEventId = lastLiveOperatorEventId,
      onlineBidCount = onlineBidCount,
      reserve = reserve,
      reserveStatus = reserveStatus,
      sellingPriceCents = sellingPriceCents,
      sellingToBidder = sellingToBidder,
      soldStatus = soldStatus,
      temporaryIncrements = temporaryIncrements,
      winningBidder = winningBidder,
      winningBidEventId = winningBidEventId
    )

    val publicForm = PublicDerivedLotState(
      askingPriceCents = askingPriceCents,
      bidCount = bidCount,
      biddingStatus = biddingStatus,
      estimate = estimate,
      floorAskingPriceCents = floorAskingPriceCents,
      floorBidCount = floorBidCount,
      floorIsComplete = floorIsComplete,
      floorIsOpen = floorIsOpen,
      floorReserveStatus = floorReserveStatus,
      floorSellingPriceCents = floorSellingPriceCents,
      floorWinningBidder = floorWinningBidder,
      floorWinningBidEventId = floorWinningBidEventId,
      isComplete = isComplete,
      isOpen = isOpen,
      onlineBidCount = onlineBidCount,
      reserveStatus = reserveStatus,
      sellingPriceCents = sellingPriceCents,
      sellingToBidder = sellingToBidder,
      soldStatus = soldStatus,
      winningBidder = winningBidder,
      winningBidEventId = winningBidEventId
    )
  }

  case class ExternalOperatorDerivedLotState(
                                              askingPriceCents: Long,
                                              bidCount: Int,
                                              biddingStatus: BiddingStatus,
                                              canArtsyDoubleBid: Boolean,
                                              estimate: Estimate,
                                              floorAskingPriceCents: Option[Long] = None,
                                              floorBidCount: Option[Int] = None,
                                              floorIsComplete: Option[Boolean] = None, // TODO deprecated
                                              floorIsOpen: Option[Boolean] = None, // TODO deprecated
                                              floorReserveStatus: Option[ReserveStatus] = None,
                                              floorSellingPriceCents: Option[Long] = None,
                                              floorWinningBidder: Option[Bidder] = None,
                                              floorWinningBidEventId: Option[EventId] = None,
                                              footingData: FootingData,
                                              incrementPolicyId: IncrementPolicyId,
                                              isComplete: Boolean, // TODO deprecated
                                              isOpen: Boolean, // TODO deprecated
                                              lastLiveOperatorEventId: Option[EventId],
                                              onlineBidCount: Option[Int] = None,
                                              reserve: Option[Reserve],
                                              reserveStatus: ReserveStatus,
                                              sellingPriceCents: Long,
                                              sellingToBidder: Option[Bidder],
                                              soldStatus: SoldStatus,
                                              temporaryIncrements: Option[TemporaryIncrementStrategy],
                                              winningBidder: Option[Bidder],
                                              winningBidEventId: Option[EventId]
                                            ) extends DerivedLotState {
    val publicForm = PublicDerivedLotState(
      askingPriceCents = askingPriceCents,
      bidCount = bidCount,
      biddingStatus = biddingStatus,
      estimate = estimate,
      floorAskingPriceCents = floorAskingPriceCents,
      floorBidCount = floorBidCount,
      floorIsComplete = floorIsComplete,
      floorIsOpen = floorIsOpen,
      floorReserveStatus = floorReserveStatus,
      floorSellingPriceCents = floorSellingPriceCents,
      floorWinningBidder = floorWinningBidder,
      floorWinningBidEventId = floorWinningBidEventId,
      isComplete = isComplete,
      isOpen = isOpen,
      onlineBidCount = onlineBidCount,
      reserveStatus = reserveStatus,
      sellingPriceCents = sellingPriceCents,
      sellingToBidder = sellingToBidder,
      soldStatus = soldStatus,
      winningBidder = winningBidder,
      winningBidEventId = winningBidEventId
    )
    val externalOperatorForm = this
  }

  case class PublicDerivedLotState(
                                    askingPriceCents: Long,
                                    bidCount: Int,
                                    biddingStatus: BiddingStatus,
                                    estimate: Estimate,
                                    floorAskingPriceCents: Option[Long] = None,
                                    floorBidCount: Option[Int] = None,
                                    floorIsComplete: Option[Boolean] = None, // TODO deprecated
                                    floorIsOpen: Option[Boolean] = None, // TODO deprecated
                                    floorReserveStatus: Option[ReserveStatus] = None,
                                    floorSellingPriceCents: Option[Long] = None,
                                    floorWinningBidder: Option[Bidder] = None,
                                    floorWinningBidEventId: Option[EventId] = None,
                                    isComplete: Boolean, // TODO deprecated
                                    isOpen: Boolean, // TODO deprecated
                                    onlineBidCount: Option[Int] = None,
                                    reserveStatus: ReserveStatus,
                                    sellingPriceCents: Long,
                                    sellingToBidder: Option[Bidder],
                                    soldStatus: SoldStatus,
                                    winningBidder: Option[Bidder],
                                    winningBidEventId: Option[EventId]
                                  ) extends DerivedLotState {
    def externalOperatorForm =
      throw new Exception(
        "Cannot go from PublicDerivedLotState to ExternalOperatorDerivedLotState"
      )
    val publicForm = this
  }

  case class FootingData(
                          amountsCents: Seq[Long],
                          currentStep: Int,
                          askingStep: Int,
                          onlineCompetitiveStep: Option[Int],
                          onlineMaxStep: Option[Int]
                        )

  case class AnnotatedIncrement(
                                 amountCents: Long,
                                 awardedTo: Option[Bidder],
                                 isConfirmed: Boolean,
                                 isAsking: Boolean,
                                 isOnlineCompetitive: Boolean,
                                 isOnlineMax: Boolean,
                                 isExpected: Boolean,
                                 isStarting: Boolean
                               )

}
