package models
// $COVERAGE-OFF$
object Constants {
  // general constants
  object AdProviderConfig {
    final val APIKey = "APIKey"
  }

  final val DefaultReportingTimeoutMs = 20000
  final val NoValue = "Value Missing"

  // Unity Ads specific constants
  object UnityAds {
    final val GameID = "gameID"
    final val Success = "1"
    final val VerifyFailure = "Verification failed"
    final val Name = "UnityAds" // This name is sent to the SDK, so the space is omitted.
    final val DisplayName = "Unity Ads" // This name is rendered in the dashboard.
    final val ReportingStarted = "started"
    final val ReportingRevenue= "revenue"

    val CallbackUrl = "/v1/reward_callbacks/%s/unity_ads?sid=[SID]&oid=[OID]&hmac=[SIGNATURE]"
    val CallbackUrlSpec= "/v1/waterfall/%s/unity_ads_completion?sid=%%sid%%&oid=%%oid%%&hmac=%%hmac%%"
  }

  object AdProvider {
    final val namePattern = "^[a-zA-z0-9]+$".r // Regex used to ensure Ad Provider names do not contain spaces or punctuation.
  }
}
// $COVERAGE-ON$
