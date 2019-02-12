package models

import models.TaggedIds.{BidderId, PaddleNumber, UserId}

/** Represents the identity of a bidder */
sealed trait Bidder {
  def isArtsyBidder: Boolean   = this.isInstanceOf[ArtsyBidder]
  def isOfflineBidder: Boolean = this == OfflineBidder
}
case class ArtsyBidder(
                        bidderId: BidderId,
                        paddleNumber: PaddleNumber,
                        userId: Option[UserId]
                      ) extends Bidder {
  require(bidderId.nonEmpty, "ArtsyBidder.bidderId must not be empty")
  require(paddleNumber.nonEmpty, "ArtsyBidder.paddleNumber must not be empty")

  // Ignore userIdOpt in comparisons
  // TODO remove when all bids have userIdOpt populated
  override def equals(obj: scala.Any): Boolean = obj match {
    case ArtsyBidder(otherBidderId, otherPaddleNumber, _)
      if bidderId == otherBidderId && paddleNumber == otherPaddleNumber =>
      true
    case _ => false
  }
}
case object OfflineBidder extends Bidder
case object NoBidder      extends Bidder
