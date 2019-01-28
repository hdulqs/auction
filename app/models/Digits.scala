package models

object Digits {

  // We use base-10 logs for digit counting, so we can operate independent
  // of magnitude, and then powers to restore magnitude.

  /**
    * Power-of-ten generator for integer math.
    * @param x the exponent
    * @return 10 raised to the power of the argument, as a Long.
    */
  def pow10Long(x: Long): Long = {
    if (x < 0)
      throw new IllegalArgumentException(
        s"10^$x is fractional, and cannot be represented as a Long"
      )
    val floatValue = math.pow(10, x)
    if (floatValue > Long.MaxValue)
      throw new IllegalArgumentException(s"10^$x is too big to fit in a Long")
    floatValue.toLong

  }

  def numDigits(x: Long): Int = (math.log10(math.abs(x)).toLong + 1).toInt

  /** Produces the lowest integer with the same number of digits. */
  def orderOfMagnitude(x: Long): Long = pow10Long(numDigits(x) - 1) * math.signum(x)

  def orderOfMagnitudeForNumDigits(n: Int): Long =
    if (n == 0) 0 else pow10Long(math.abs(n) - 1) * math.signum(n)

  /**
    * Produces the desired number of leading digits from an input number. So
    * 85,100 becomes 8 with one leading digit, 85 with two leading digits, and
    * 851 with three leading digits.
    * @param x the input number.
    * @param count the leading digits to preserve.
    * @return the leading digits of the input number.
    * @throws IllegalArgumentException if count < 0 or > the number of digits in
    *                                  x.
    */
  def leadingDigits(x: Long, count: Int): Long = {
    require(count > 0, "`count` must be greater than 0")
    require(
      count <= numDigits(x),
      s"`count` must be less than the number of digits in `x` (${numDigits(x)}"
    )

    math.abs(x) / orderOfMagnitudeForNumDigits(numDigits(x) - count + 1)
  }

  /**
    * Rounds an integer down to the desired number of significant figure. So
    * 85,100 becomes 80,000 with 1 sig fig, 85,000 with 2 sig figs and
    * 85,100 with 3+ sig figs.
    * @param x the input number.
    * @param count the number of sig figs to preserve.
    * @return the rounded output.
    * @throws IllegalArgumentException if count < 0
    */
  def roundToSigFigs(x: Long, count: Int): Long = {
    val n = numDigits(x)
    if (count < n)
      leadingDigits(x, count) * pow10Long(n - count) * math.signum(x)
    else x
  }
}
