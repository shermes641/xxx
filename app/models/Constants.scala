package models

// $COVERAGE-OFF$
//@formatter:off
object Constants {

  // these will be exit codes when we stopp the app from booting
  object Errors {
    final val AdProviderError     = -200
    final val KeenConfigError     = -100
  }

  // general constants
  object PlatformConfig {
    final val Ios     = "ios"
    final val Android = "android"
  }

  object JunGroup {
    final val Token   = "jungroup.token"
    final val Url     = "jungroup.url"
    final val User    = "jungroup.user"
    final val Pw      = "jungroup.password"
    final val Email   = "jungroup.email"
  }

  object AppConfig {
    final val Mode                      = "mode"
    final val Staging                   = "staging"
    final val AuthFailureDelayMs        = "authentication_failure_delay"
    final val AppDomain                 = "app_domain"
    final val S2sIosCallbackDomain      = "ios_server_to_server_callback_domain"
    final val S2sAndroidCallbackDomain  = "android_server_to_server_callback_domain"
    final val PlayerCallbackUrl         = "jungroup.callbackurl"
    final val OpsEmail                  = "hyprmediate_ops_email"
    final val TeamEmail                 = "hyprmarketplace.team_email"
    final val HttpAuthUser              = "httpAuthUser"
    final val HttpAuthPw                = "httpAuthPassword"
    final val WebDriverType             = "webDriverType"
  }

  object Hmac {
    final val SignerAlgorithm           = "signerAlgorithm"
    final val SignerSeparator           = "signerSeparator"
    final val SignerTolerance           = "signerTolerance"
  }

  object KeenConfig {
    final val ProjectsUrl             = "https://api.keen.io/3.0/projects/"
    final val prefix                  = "keen."
    final val ProjectID               = "id"
    final val OrgID                   = "keen_org_id"
    final val OrgKey                  = "keen_org_key"
    final val Project                 = "project"
    final val WriteKey                = "writeKey"
    final val ReadKey                 = "readKey"
    final val MasterKey               = "masterKey"
    final val ErrorProjectID          = "keen.errorReportingProject"
    final val ErrorProjectKey         = "keen.errorReportingWriteKey"
    final val ReviewErrorProjectID    = "keen_review_error_project"
    final val ReviewErrorProjectKey   = "keen_review_error_key"
  }

  object AdProviderConfig {
    final val IosID                   = 2
    final val AndroidID               = 6
    final val AppID                   = "appID"
    final val APIKey                  = "APIKey"
    final val IosAdProviderID         = "hyprmarketplace.ios_ad_provider_id"
    final val AndroidAdProviderID     = "hyprmarketplace.android_ad_provider_id"
  }

  final val DefaultReportingTimeoutMs = 20000
  final val NoValue = ""

  // Unity Ads specific constants
  final val UnityAdsSuccess = "1"
  final val UnityAdsVerifyFailure = "Verification failed"
  final val UnityAdsName = "UnityAds" // This name is sent to the SDK, so the space is omitted.
  final val UnityAdsDisplayName = "Unity Ads" // This name is rendered in the dashboard.
  val UnityAdsCallbackUrl = "/v1/reward_callbacks/%s/unity_ads?sid=[SID]&oid=[OID]&hmac=[SIGNATURE]"
  val UnityAdsCallbackUrlSpec= "/v1/waterfall/%s/unity_ads_completion?sid=%%sid%%&oid=%%oid%%&hmac=%%hmac%%"
  final val UnityAdsReportingStarted = "started"
  final val UnityAdsReportingRevenue= "revenue"

  object AdProvider {
    final val namePattern = "^[a-zA-z0-9]+$".r // Regex used to ensure Ad Provider names do not contain spaces or punctuation.
  }

  object HerokuConfigVars {
    final val ParentName          = "heroku_parent_app_name"
    final val AppName             = "heroku_app_name"
    final val LatestSha           = "latestSHA"     // TODO will probably replace this with HEROKU_SLUG_COMMIT
    final val LatestBranch        = "latestBranch"  // TODO will probably replace this with HEROKU_RELEASE_VERSION or HEROKU_SLUG_DESCRIPTION
  }

  object Reporting {
    final val HyprMarketplaceUrl  = "hyprmarketplace.reporting_url"
    final val AdcolonyUrl         = "adcolony.reporting_url"
    final val ApplovinUrl         = "applovin.reporting_url"
    final val VungleUrl           = "vungle.reporting_url"
    final val UnityadsUrl         = "unityads.reporting_url"
  }
}
//@formatter:off
// $COVERAGE-ON$
