package hmac
// $COVERAGE-OFF$
//@formatter:off
object HmacConstants {
  // Used to allow missing or invalid HMAC signatures on requests
  //TODO when we care about signing these will be changed to error messages
  final val MissingOrInvalidHmacSignature = true
  final val TimestampTooOld               = true

  final val HttpsPort             = 443
  final val HttpPort              = 80
  final val UTF8                  = "UTF8"

  // map key constants
  final val AdProviderRequest     = "original_postback"
  final val AdProviderName        = "ad_provider"
  final val AdProviderUser        = "user_id"
  final val RewardQuantity        = "reward_quantity"
  final val OfferProfit           = "estimated_offer_profit"
  final val TransactionID         = "transaction_id"
  final val TimeStamp             = "time_stamp"
  final val OriginalPostback      = "original_postback"

  // Query string constants
  final val QsHmac                = "hmac"
  final val QsVersionKey          = "version"
  final val QsVersionValue1_0     = "1.0"

  // Sane defaults if config is missing. These values are also used in tests
  final val DefaultUid            = "no uid"
  final val DefaultAdProviderName = "no provider name"
  final val DefaultTransactionId  = "no trans id"
  final val DefaultSecret         = "some secret only for testing"
  final val DefaultAlgorithm      = "HmacSHA256"
  final val DummyUrl              = "http://localhost:9000/dummy"
  final val DefaultMethod         = "POST"

  final val DefaultAppToken       = "default app token"
  final val DefaultOfferProfit    = 0.5
  final val DefaultRewardQuantity = 2L
  final val DefaultTimeStamp      = 123456789L
}
//@formatter:on
// $COVERAGE-ON$