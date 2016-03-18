package hmac

object Utils {

  /**
    * Converts an hmac tolerance value to a number of seconds
    * The return value is typically used to test the age of a hmac signed request
    *
    * @param tolerance The time tolerance set in app config
    * @return Typical return values are 1, 10, 100, 1000 ......
    */
  def toleranceToSecs(tolerance: Double = Signer.tolerance) = {
    val lowerLimit = 3
    val base = 10
    val minWindowSecs = 1L

    tolerance match {
      case t if Math.floor(tolerance) < lowerLimit =>
        minWindowSecs

      case _ =>
        scala.math.pow(base, Math.floor(tolerance) - lowerLimit).toLong
    }
  }
}