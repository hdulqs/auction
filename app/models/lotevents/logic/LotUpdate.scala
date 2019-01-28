package models.lotevents.logic

import java.time.ZonedDateTime

import models.TaggedIds._
import models.{PrivilegeLevel, UserCredentials}
import models.lotevents.LotEvent.PastEvents
import models.lotevents.logic.LotState.DerivedLotState
import models.lotevents.{LotEvent, LotEventValidationMeta, RejectionReason, StoredLotEvent}
import play.api.libs.json.JsObject

case class EventForLot(
                        event: LotEvent,
                        lotId: LotId,
                        clientMetaData: Option[JsObject],
                        credentials: Option[UserCredentials],
                        source: Option[CommandSource]
                      ) {
  require(lotId.nonEmpty, "EventForLot.lotId must not be empty")
}

/**
  * This data is used to track the command that initiated an event. This is or
  * can be used for a number of purposes.
  * - Allows a client to correlate a command it sent with the updates that
  *   were directly caused by that command.
  * - Potentially allows for commands to result in multiple events being
  *   persisted, providing a unifying key to correlate the events.
  * - Allows us to measure the latency within Causality between an initial
  *   command being received and all resulting updates being queued.
  */
case class CommandSource(
                          key: CommandKey,
                          receivedAt: ZonedDateTime
                        )

case class Outcome(
                    storedEvent: StoredLotEvent,
                    wasAccepted: Boolean,
                    reason: Option[RejectionReason]
                  )

/** Parent type of async updates pushed from the app logic. */
abstract class Update extends Serializable

/**
  * Response that indicates whether the incoming event was accepted.
  *
  * @param wasAccepted true iff validation succeeded before the timeout
  * @param prospectiveEvent the event sent in the original request
  * @param storedEventList all accepted events at the time of response (including
  *                  the prospective one, in the final position, iff accepted)
  * @param reason the validation reason
  */
case class EventStorageOutcome(
                                wasAccepted: Boolean,
                                prospectiveEvent: LotEvent,
                                storedEventList: PastEvents,
                                reason: Option[RejectionReason],
                                meta: Option[LotEventValidationMeta]
                              )

case class LotUpdate(
                      lotId: LotId,
                      saleId: SaleId,
                      events: Map[EventId, StoredLotEvent],
                      partialEventHistory: PastEvents, // TODO deprecated, remove when clients are compatible
                      fullEventOrder: Seq[EventId],
                      derivedState: DerivedLotState,
                      sourceTrackingOpt: Option[CommandSource],
                      privilegeLevel: PrivilegeLevel
                    ) extends Update

case class MaxBidChangeUpdate(
                               lotId: LotId,
                               saleId: SaleId,
                               bidderId: BidderId,
                               amountCentsOpt: Option[Long]
                             ) extends Update
