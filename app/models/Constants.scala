package models
// $COVERAGE-OFF$
object Constants {
  // general constants
  object AdProviderConfig {
    final val AppID = "appID"
    final val APIKey = "APIKey"
  }

  final val DefaultReportingTimeoutMs = 10000
  final val NoValue = "Value Missing"

  // Unity Ads specific constants
  final val UnityAdsSuccess = "1"
  final val UnityAdsVerifyFailure = "Verification failed"
  final val UnityAdsName = "UnityAds"
  final val UnityAdsNumberOfCallbackQueryParams = 3
  val UnityAdsCallbackUrl = "/v1/reward_callbacks/%s/unity_ads?sid=[SID]&oid=[OID]&hmac=[SIGNATURE]"
  val UnityAdsCallbackUrlSpec= "/v1/waterfall/%s/unity_ads_completion?sid=%%sid%%&oid=%%oid%%&hmac=%%hmac%%"
  final val UnityAdsReportingStarted = "started"
  final val UnityAdsReportingRevenue= "revenue"
}
// $COVERAGE-ON$
