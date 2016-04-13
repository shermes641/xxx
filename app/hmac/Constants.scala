package hmac
// $COVERAGE-OFF$
object Constants {

  // Used to allow missing or invalid HMAC signatures on requests
  //TODO when we care about signing these will be changed to error messages
  final val missingOrInvalidHmacSignature = true
  final val timestampTooOld = true

  final val httpsPort = 443
  final val httpPort = 80

  //@formatter:off     map key constants
  final val adProviderRequest     = "original_postback"
  final val adProviderName        = "ad_provider"
  final val rewardQuantity        = "reward_quantity"
  final val offerProfit           = "estimated_offer_profit"
  final val transactionID         = "transaction_id"

  // Sane defaults if config is missing. These values are also used in tests
  final val DefaultUid            = "no uid"
  final val DefaultAdProviderName = "no provider name"
  final val DefaultTransactionId  = "no trans id"
  final val DefaultSecret         = "some secret only for testing"
  final val DefaultAlgorithm      = "HmacSHA256"
  final val DefaultSeparator      = "+"
  final val DefaultTolerance      = 4.1
  final val dummyUrl              = "http://localhost:9000/dummy"
  final val DefaultNonce          = "1234ADVfgtr^^&087="
  final val DefaultMethod         = "POST"

  final val DefaultAppToken       = "default app token"
  final val DefaultOfferProfit    = 0.5
  final val DefaultRewardQuantity = 2L
  //@formatter:on
}
// $COVERAGE-ON$