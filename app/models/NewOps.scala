package models

/** Mixin for helpers for common operations. */
object NewOps {
  implicit class NewSeqOps[A](val seq: Seq[A]) extends AnyVal {

    /** Same as `find`, but in reverse order. */
    def findLast(p: A => Boolean): Option[A] = seq.reverse.find(p)

    /** Same as `collectFirst`, but in reverse order. */
    def collectLast[B](pf: PartialFunction[A, B]): Option[B] =
      seq.reverse.collectFirst(pf)

    /**
      * Returns true if the partial function matches an item in the collection.
      *
      * @param pf the matcher; its return value is ignored.
      */
    def hasMatching(pf: PartialFunction[A, _]): Boolean =
      seq.collectFirst(pf).isDefined

    /** Same as `splitAt`, but in reverse order */
    def splitAtRev(n: Int): (Seq[A], Seq[A]) = {
      seq.splitAt(seq.length - n)
    }

    /** Same as `span`, but in reverse order */
    def spanRev(f: A => Boolean): (Seq[A], Seq[A]) = {
      val (seqAfterRev, seqBeforeRev) = seq.reverse.span(f)
      (seqBeforeRev.reverse, seqAfterRev.reverse)
    }

    /**
      * Find all items in a collection that match the provided partial function,
      * and replace them with the output of the partial function. If no match is
      * found, an identical sequence will be returned.
      *
      * Each replacement item could be computed from the matched item, or it
      * could be a constant.
      *
      * @param pf matcher and replacer function.
      * @return the new sequence.
      */
    def matchAndReplace(pf: PartialFunction[A, A]): Seq[A] = {
      val fOpt = pf.lift
      seq.map(item => fOpt(item).getOrElse(item))
    }

    /**
      * Find the first item in a collection that matches the provided partial
      * function, and replace it with the output of the partial function. If no
      * match is found, an identical sequence will be returned.
      * @param pf matcher and replacer function.
      * @return the new sequence.
      */
    def matchAndReplaceFirst(pf: PartialFunction[A, A]): Seq[A] = {
      val fOpt                     = pf.lift
      val (before, targetAndAfter) = seq.span(fOpt(_).isEmpty)
      val (target, after)          = targetAndAfter.splitAt(1)
      val targetModified           = target.flatMap(fOpt(_)) // If target exists, targetModified will too.
      before ++ targetModified ++ after
    }

    /**
      * Find the last item in a collection that matches the provided partial
      * function, and replace it with the output of the partial function. If no
      * match is found, an identical sequence will be returned.
      * @param pf matcher and replacer function.
      * @return the new sequence.
      */
    def matchAndReplaceLast(pf: PartialFunction[A, A]): Seq[A] = {
      val fOpt                     = pf.lift
      val (beforeAndTarget, after) = seq.spanRev(fOpt(_).isEmpty)
      val (before, target)         = beforeAndTarget.splitAtRev(1)
      val targetModified           = target.flatMap(fOpt(_)) // If target exists, targetModified will too.
      before ++ targetModified ++ after
    }

    /**
      * Simpler version of matchAndReplace, for when the new item is independent
      * of the target item. Replaces all matching occurrences of the item.
      * @param f predicate that is true for the target item.
      * @param newItem the item to replace a target item with.
      * @return the list with substituted items, if the target was found, or the
      *         input list, otherwise.
      */
    def findAndReplace(f: A => Boolean, newItem: A): Seq[A] = matchAndReplace {
      case x if f(x) => newItem
    }

    /**
      * Simpler version of matchAndReplaceFirst, for when the new item is
      * independent of the target item. Replaces only the first occurrence of
      * the item.
      * @param f predicate that is true for the target item.
      * @param newItem the item to replace a target item with.
      * @return the list with a substitute item, if the target was found, or the
      *         input list, otherwise.
      */
    def findAndReplaceFirst(f: A => Boolean, newItem: A): Seq[A] = {
      matchAndReplaceFirst { case x if f(x) => newItem }
    }

    /**
      * Simpler version of matchAndReplaceLast, for when the new item is
      * independent of the target item. Replaces only the last occurrence of the
      * item.
      * @param f predicate that is true for the target item.
      * @param newItem the item to replace a target item with.
      * @return the list with a substitute item, if the target was found, or the
      *         input list, otherwise.
      */
    def findAndReplaceLast(f: A => Boolean, newItem: A): Seq[A] = {
      matchAndReplaceLast { case x if f(x) => newItem }
    }

    /** Safe version of `max`, which doesn't throw an exception */
    def maxOption(implicit cmp: scala.Ordering[A]): Option[A] =
      if (seq.isEmpty) None else Some(seq.max)

    /**
      * Thin layer on foldLeft to accomodate a situation where there's some
      * internal state in the fold to propagate, but you only care about a
      * result value.
      * @param z the initial result
      * @param initialState this initial state
      * @param op a reducer that takes the previous result, state and the
      *           current item to produce the next result and state.
      * @tparam B the result type
      * @tparam C the state type
      * @return just the result
      */
    def foldLeftWithState[B, C](z: B, initialState: C)(
      op: (B, C, A) => (B, C)
    ): B = {
      seq
        .foldLeft((z, initialState)) {
          case ((prevResult, prevInternal), current) =>
            op(prevResult, prevInternal, current)
        }
        ._1
    }
  }

  implicit class NewSeqPairOps[A, B](val seq: Seq[(A, B)]) extends AnyVal {

    /**
      * Common (but confusing) groupBy application of a list of pairs into a
      *  map that collects the second item. Basically, a groupBy that discards
      *  the keys in the output side of the map.
      */
    def groupLastsByFirsts: Map[A, Seq[B]] =
      seq.groupBy(_._1).mapValues(_.map(_._2))
  }

  implicit class NewOptionOps[A](val opt: Option[A]) extends AnyVal {

    /**
      * Like Option.getOrElse, except it also returns the fallback if its less
      * than the contained value.
      */
    def getIfLessThan[B >: A](fallback: B)(implicit ordering: Ordering[B]): B =
      opt match {
        case Some(a) => if (ordering.lt(a, fallback)) a else fallback
        case _       => fallback
      }

    /**
      * Like Option.getOrElse, except it also returns the fallback if its
      * greater than the contained value.
      */
    def getIfGreaterThan[B >: A](
                                  fallback: B
                                )(implicit ordering: Ordering[B]): B = opt match {
      case Some(a) => if (ordering.gt(a, fallback)) a else fallback
      case _       => fallback
    }
  }

  implicit class AnyOps[A](val value: A) extends AnyVal {

    def tee(op: A => Unit): A = {
      // Allow for partial functions to be used.
      try { op(value) } catch { case _: MatchError => }
      value
    }
  }
}
