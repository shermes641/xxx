package hmac

/**
  * Created by shermes on 1/28/16.
  *
  * Verify misc functions
  */
class ConstantsSpec extends BaseSpec {

  val tolerance =
    Table(("tolerance", "result"), // First tuple defines column names
      (1.0, 1L),
      (1.5, 1L),
      (1.99999999999, 1L),
      (2.0, 1L),
      (2.5, 1L),
      (2.99999999999, 1L),
      (3.0, 1L),
      (3.5, 1L),
      (3.99999999999, 1L),
      (4.0, 10L),
      (4.5, 10L),
      (4.99999999999, 10L),
      (5.0, 100L),
      (5.5, 100L),
      (5.99999999999, 100L),
      (6.0, 1000L),
      (6.5, 1000L),
      (6.99999999999, 1000L),
      (7.0, 10000L),
      (7.0, 10000L),
      (7.5, 10000L),
      (7.99999999999, 10000L),
      (8.0, 100000L),
      (8.5, 100000L),
      (8.99999999999, 100000L),
      (9.0, 1000000L),
      (9.5, 1000000L),
      (9.99999999999, 1000000L),
      (10.0, 10000000L),
      (10.5, 10000000L),
      (10.99999999999, 10000000L)
    )

  /**
    * tolerance is used to create a time window for responses to HMAC signed requests.
    * tolerance creates a window of 1, 10, 100, 1000 ....  seconds
    * We do not currently expect or validate any recieved requests that may or may not be HMAC signed
    */
  property("converting tolerance to seconds should pass") {
    forAll(tolerance) { (tolerance: Double, result: Long) =>
      Utils.toleranceToSecs(tolerance) shouldEqual result
    }
  }
}

