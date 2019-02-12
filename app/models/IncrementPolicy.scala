package models

import java.time.ZonedDateTime

import IncrementPolicy._
import NextIncrementRule._
import NewOps._
import models.TaggedIds._
import tagging._

/** Mixin with shared logic for enumerating increments. */
trait EnumerableIncrements {

  /**
    * Helper for returning an increment above a given amount.
    *
    * @param fromAmountCents the base amount.
    * @return the base amount plus the applicable increment.
    */
  def addIncrement(
                    fromAmountCents: Long,
                    nextIncrementRule: NextIncrementRule
                  ): Long

  /**
    * Generator for an arbitrarily long sequence of increments. Note that when
    * using SnapToPresetIncrements, the resulting sequence will be calculated
    * from zero, even if a different startCents is provided. The starting
    * amount is always included.
    */
  def enumerateIncrements(
                           startCents: Long = 0,
                           nextIncrementRule: NextIncrementRule = SnapToPresetIncrements
                         ): Stream[Long] = {
    // At most, the first increment is affected by whether we snap to presets
    // and from there, we'll just be adding full increments.
    val nextAmountCents = addIncrement(startCents, nextIncrementRule)
    Stream(startCents) ++ Stream.iterate(nextAmountCents)(
      addIncrement(_, AddToPastValue)
    )
  }

  def enumerateIncrementsWithLimit(
                                    from: Long = 0,
                                    until: Long,
                                    nextIncrementRule: NextIncrementRule = SnapToPresetIncrements
                                  ): Stream[Long] = {
    enumerateIncrements(from, nextIncrementRule).takeWhile(_ <= until)
  }
}

/**
  * Controls the future asking prices for a lot. Nominally, a lot follows a
  * preset increment policy, but auctioneers can deviate to other amounts.
  * The former situation is represented by the subclass IncrementPolicy and
  * the latter by ConstantIncrement and PatternIncrement. This intentionally
  * excludes NewIncrementPolicy, which is only used for input, not
  * persistence.
  */
sealed trait IncrementStrategy extends EnumerableIncrements

/**
  * Mixin with shared calculation logic for IncrementPolicy and
  * NewIncrementPolicy.
  */
trait IncrementPolicyLogic {
  self: EnumerableIncrements =>

  def initialIncrementCents: Long
  def changes: Seq[IncrementPolicy.Change]

  /**
    * If we create other formats of changes to store in the database, the idea
    * is to convert them into fixed changes. So that the rest of the logic
    * stays simple.
    */
  val normalizedChanges: Seq[FixedChange] = changes.map {
    case fixedChange: FixedChange => fixedChange
  }

  /**
    * Rather than constantly counting up from zero to find every increment,
    * use a lazy `Stream` to do this only once per initialization of the
    * `IncrementStrategy`. Propagating the remaining change data with the
    * amounts as their generated saves a little effort each iteration.
    */
  val amountsFromZero: Stream[Long] = Stream
    .iterate((0L, initialIncrementCents, normalizedChanges)) {
      case (prevAmountCents, incrementCents, upcomingChanges) =>
        val nextAmountCents = prevAmountCents + incrementCents
        val (pastChanges, nextRemainingChanges) =
          upcomingChanges.span(_.thresholdCents <= nextAmountCents)
        val nextIncrementCents = pastChanges.lastOption
          .map(_.incrementCents)
          .getOrElse(incrementCents)
        (nextAmountCents, nextIncrementCents, nextRemainingChanges)
    }
    .map(_._1)

  def incrementAtPrice(amountCents: Long): Long = {
    normalizedChanges
      .findLast(amountCents >= _.thresholdCents)
      .map(_.incrementCents)
      .getOrElse(initialIncrementCents)
  }

  def addIncrement(
                    fromAmountCents: Long,
                    nextIncrementRule: NextIncrementRule
                  ): Long = {
    nextIncrementRule match {
      case SnapToPresetIncrements =>
        amountsFromZero.dropWhile(_ <= fromAmountCents).head
      case AddToPastValue =>
        fromAmountCents + incrementAtPrice(fromAmountCents)
    }
  }

  // When increment policies were hard coded, we had configurable tests of
  // the properties below. Now, we allow clients to submit draft increment
  // policies so the user can approve the results of these diagnostic tests
  // before persisting the increment policy.

  protected lazy val lastThreshold: Long = changes
    .maxBy(_.thresholdCents)
    .thresholdCents

  protected lazy val increments: List[Long] = enumerateIncrements()
    .takeWhile(_ <= addIncrement(lastThreshold, SnapToPresetIncrements))
    .toList

  protected lazy val minAndMaxChangePercent: (Double, Double) = increments
    .dropWhile(_ < 100000L) // Percents aren't that meaningful till $1000
    .sliding(2)
    .foldLeft((Double.MaxValue, Double.MinValue)) {
      case ((prevMin, prevMax), List(currentIncrement, nextIncrement)) =>
        val percentIncrease = (nextIncrement - currentIncrement).toDouble / currentIncrement * 100
        (
          math.min(prevMin, percentIncrease),
          math.max(prevMax, percentIncrease)
        )
    }

  def enumeratedIncrements: List[List[Long]] =
    increments
      .groupBy(Digits.roundToSigFigs(_, 1))
      .toList
      .sortBy(_._1)
      .map(_._2)

  def minChangePercent: Double = minAndMaxChangePercent._1
  def maxChangePercent: Double = minAndMaxChangePercent._2

  /**
    * True, if incrementing from zero lands exactly on each threshold. Some
    * increment policies do not have this property, but this is rare.
    */
  def landsOnThresholds: Boolean = {
    val thresholds = changes.map(_.thresholdCents)
    val preThresholdIncrements = amountsFromZero
      .takeWhile(_ <= thresholds.last)
      .sliding(2)
      .collect {
        case Stream(item, next) if thresholds.contains(next) => item
      }
      .toList
    preThresholdIncrements.map(addIncrement(_, AddToPastValue)) == thresholds
  }

  /**
    * Calculates the min and max percentage changes of each asking amount.
    * Most increment policies have changes that range between 4% and 10%.
    */
  def incrementPercentageRange: (Double, Double) = {
    amountsFromZero
      .dropWhile(_ < 100000L) // This rule doesn't really kick in till 1000 bucks
      .sliding(2)
      .foldLeft((Double.MaxValue, 0d)) {
        case (
          (prevMinChange, prevMaxChange),
          Stream(currentIncrement, nextIncrement)
          ) =>
          val percentIncrease = (nextIncrement - currentIncrement).toDouble / currentIncrement * 100
          (
            Math.min(prevMinChange, percentIncrease),
            Math.max(prevMaxChange, percentIncrease)
          )
      }
  }
}

/**
  * @param incrementPolicyId the canonical ID for an increment policy revision.
  * @param groupTag the tag for the group (e.g partner) that an increment
  *                 policy applies to.
  * @param createdAt the timestamp of the increment policy, used to
  *                  preferentially surface the most recent revision.
  * @param subgroupTag the subgroup of an increment policy, for groups that
  *                    have multiple revisions in effect.
  * @param initialIncrementCents the increment from zero.
  * @param changes the progression of thresholds and new increments.
  */
case class IncrementPolicy(
                            incrementPolicyId: IncrementPolicyId,
                            groupTag: GroupTag,
                            subgroupTag: SubgroupTag,
                            createdAt: ZonedDateTime,
                            initialIncrementCents: Long,
                            changes: Seq[IncrementPolicy.Change]
                          ) extends IncrementStrategy
  with IncrementPolicyLogic {
  require(
    incrementPolicyId.nonEmpty,
    "IncrementPolicy.incrementPolicyId must not be empty"
  )
  require(groupTag.nonEmpty, "IncrementPolicy.groupTag must not be empty")
  require(
    subgroupTag.nonEmpty,
    "IncrementPolicy.subgroupTag must not be empty (try 'Default')"
  )
  require(
    initialIncrementCents > 0,
    "IncrementPolicy.initialIncrementCents must be > 0"
  )
  require(
    changes.groupBy(_.thresholdCents).forall(_._2.length == 1),
    "IncrementPolicy.changes must be unique by threshold"
  )
  require(
    changes == changes.sortBy(c => c.thresholdCents),
    "IncrementPolicy.changes must be sorted by threshold"
  )
}

object IncrementPolicy {
  sealed trait Change { def thresholdCents: Long }
  case class FixedChange(thresholdCents: Long, incrementCents: Long)
    extends Change {
    require(thresholdCents > 0, "FixedChange.thresholdCents must be > 0")
    require(incrementCents > 0, "FixedChange.incrementCents must be > 0")
  }

  // This exists in case of lookup failures, but should never happen. We log it in
  // AuctionCalculator.incrementStrategy if it occurs.
  val fallbackString                = "fallback"
  val fallbackInitialIncrementCents = 10000
  object FallbackIncrementPolicy
    extends IncrementPolicy(
      fallbackString.taggedWith[IncrementPolicyIdTag],
      fallbackString.taggedWith[GroupTagTag],
      fallbackString.taggedWith[SubgroupTagTag],
      ZonedDateTime.now(),
      fallbackInitialIncrementCents,
      Seq.empty
    )
}

case class IncrementPolicyGroup(
                                 groupTag: GroupTag,
                                 subgroupTags: Seq[SubgroupTag]
                               )

case class IncrementPolicySubgroup(
                                    subgroupTag: SubgroupTag,
                                    group: IncrementPolicyGroup,
                                    revisions: Seq[IncrementPolicy]
                                  )

sealed trait TemporaryIncrementStrategy extends IncrementStrategy

/**
  * Captures the concept of always incrementing an exact amount from a given
  * price.
  */
case class ConstantIncrement(amountCents: Long)
  extends TemporaryIncrementStrategy {
  require(amountCents > 0, "ConstantIncrement.amountCents must be > 0")

  def addIncrement(
                    fromAmountCents: Long,
                    nextIncrementRule: NextIncrementRule
                  ): Long = fromAmountCents + amountCents
}

/**
  * Captures the concept of increments that snap to a preset spacing pattern.
  * @param startingAmountCents the amount where the pattern should start. The
  *                            magnitudes of each further amount will be
  *                            scaled to this value.
  * @param stopPattern a list of relative amounts. For 2-5-8 increments, this
  *                    would be .2, .5, .8. Implicitly, this ends with a 1.
  */
case class PatternIncrement(
                             startingAmountCents: Long,
                             stopPattern: Seq[BigDecimal]
                           ) extends TemporaryIncrementStrategy {

  import PatternIncrement.minimumStartingAmountCents

  // Algorithm doesn't make sense below 10
  require(
    startingAmountCents > minimumStartingAmountCents,
    s"PatternIncrement.startingAmountCents must be > $minimumStartingAmountCents"
  )
  require(
    stopPattern.forall(_ > 0),
    "PatternIncrement.stopPattern must each be > 10"
  )
  require(
    stopPattern.nonEmpty,
    "PatternIncrement.stopPattern must be non-empty"
  )

  // Base amount which would increment the second-most significant digit by
  // 1. E.g. 10 for 100, 1,000 for 43,000, and 1,000 for 95,000.
  val incrementUnit: Long = Digits.orderOfMagnitude(startingAmountCents)
  val thresholdDigits: Int = Digits.numDigits(startingAmountCents)

  def addIncrement(
                    fromAmountCents: Long,
                    nextIncrementRule: NextIncrementRule
                  ): Long = {
    // Suppose startingAmountCents = 25342 and amountCents = 143,123, and
    // we're doing 2-5-8-0s. stopPattern would be .2, .5, .8. incrementUnit
    // would be 1,000.

    val baseAmount = Digits.roundToSigFigs(
      fromAmountCents,
      Digits.numDigits(fromAmountCents) - thresholdDigits + 1
    ) // 140,000
    val remnant         = fromAmountCents - baseAmount        // 3,123
    val remnantFraction = BigDecimal(remnant) / incrementUnit // .3
    val newRemnantFraction =
      stopPattern.find(_ > remnantFraction).getOrElse(BigDecimal(1)) // .5
    val newRemnant = (newRemnantFraction * incrementUnit).toLong // 5,000
    baseAmount + newRemnant // 145,000.
  }
}

object PatternIncrement {
  val `0-2-5-8`: Seq[BigDecimal] = Seq(BigDecimal(".2"), BigDecimal(".5"), BigDecimal(".8"))
  val minimumStartingAmountCents = 10L
}