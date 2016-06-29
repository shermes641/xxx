package models

// $COVERAGE-OFF$
//@formatter:off
object Constants {

  final val KeenPrefix    = "keen"
  final val JunPrefix     = "jungroup"

  // these will be exit codes when we stop the app from booting
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
    final val Token   = s"$JunPrefix.token"
    final val Url     = s"$JunPrefix.url"
    final val User    = s"$JunPrefix.user"
    final val Pw      = s"$JunPrefix.password"
    final val Email   = s"$JunPrefix.email"
  }

  object AppConfig {
    final val Mode                      = "mode"
    final val Staging                   = "staging"
    final val AuthFailureDelayMs        = "authentication_failure_delay"
    final val AppDomain                 = "app_domain"
    final val S2sIosCallbackDomain      = "ios_server_to_server_callback_domain"
    final val S2sAndroidCallbackDomain  = "android_server_to_server_callback_domain"
    final val PlayerCallbackUrl         = s"$JunPrefix.callbackurl"
    final val OpsEmail                  = "hyprmediate_ops_email"
    final val TeamEmail                 = "hyprmarketplace.team_email"
    final val HttpAuthUser              = "httpAuthUser"
    final val HttpAuthPw                = "httpAuthPassword"
    final val WebDriverType             = "webDriverType"
  }

  object Hmac {
    final val SignerAlgorithm           = "signerAlgorithm"
  }

  object KeenConfig {
    final val ProjectsUrl             = s"https://api.$KeenPrefix.io/3.0/projects/"
    final val ProjectID               = "id"
    final val OrgID                   = s"${KeenPrefix}_org_id"
    final val OrgKey                  = s"${KeenPrefix}_org_key"
    final val Project                 = "project"
    final val WriteKey                = "writeKey"
    final val ReadKey                 = "readKey"
    final val MasterKey               = "masterKey"
    final val ErrorProjectID          = s"$KeenPrefix.errorReportingProject"
    final val ErrorProjectKey         = s"$KeenPrefix.errorReportingWriteKey"
    final val ReviewErrorProjectID    = s"${KeenPrefix}_review_error_project"
    final val ReviewErrorProjectKey   = s"${KeenPrefix}_review_error_key"
  }

  object AdProviderConfig {
    final val IosID                   = 2
    final val AndroidID               = 6
    final val AppID                   = "appID"
    final val APIKey                  = "APIKey"
    final val IosAdProviderID         = "hyprmarketplace.ios_ad_provider_id"
    final val AndroidAdProviderID     = "hyprmarketplace.android_ad_provider_id"
    final val ReportingParams         = "reportingParams"
    final val RequiredParams          = "requiredParams"

    val CallbackUrlDescription        = "Copy and paste this URL into the Callback URL field of %s's dashboard."
  }

  final val DefaultReportingTimeoutMs = 20000
  final val NoValue                   = ""

  // Unity Ads specific constants
  object UnityAds {
    final val GameID                  = "gameID"
    final val Success                 = "1"
    final val VerifyFailure           = "Verification failed"
    final val Name                    = "UnityAds" // This name is sent to the SDK, so the space is omitted.
    final val DisplayName             = "Unity Ads" // This name is rendered in the dashboard.
    final val ReportingStarted        = "started"
    final val ReportingRevenue        = "revenue"

    val CallbackUrl                   = "/v1/reward_callbacks/%s/unity_ads?sid=[SID]&oid=[OID]&hmac=[SIGNATURE]"
    val CallbackUrlSpec               = "/v1/waterfall/%s/unity_ads_completion?sid=%%sid%%&oid=%%oid%%&hmac=%%hmac%%"
  }

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

  object Admin {
    final val validEmail = "(?i)@(jungroup|hyprmx).com".r
  }
}
//@formatter:on
// $COVERAGE-ON$
