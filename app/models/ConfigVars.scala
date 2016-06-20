package models

import play.api.{Logger, Play}

//TODO fix up toString methods --- add test coverage
trait ConfigVars {
  //@formatter:off
  // !!! the Heroku app must enable these environ vars to be enable review apps !!!
  // RUN: heroku labs:enable runtime-dyno-metadata --app <app name>
  // and restart the app. Reference: https://devcenter.heroku.com/articles/dyno-metadata

  //Review app specific
  lazy final val ParentName  = Play.current.configuration.getString(Constants.HerokuConfigVars.ParentName).getOrElse("")

  // general application vars -- these never change for app or review app
  lazy final val AppName                         = Play.current.configuration.getString(Constants.HerokuConfigVars.AppName).get
  lazy final val LatestSha                       = Play.current.configuration.getString(Constants.HerokuConfigVars.LatestSha).get
  lazy final val LatestBranch                    = Play.current.configuration.getString(Constants.HerokuConfigVars.LatestBranch).get

  private lazy final val Staging                 = Play.current.configuration.getString(Constants.AppConfig.Staging).getOrElse("")

  private lazy final val JunGroupUrl             = Play.current.configuration.getString(Constants.JunGroup.Url).get
  private lazy final val JunGroupToken           = Play.current.configuration.getString(Constants.JunGroup.Token).get
  private lazy final val JunGroupUser            = Play.current.configuration.getString(Constants.JunGroup.User).get
  private lazy final val JunGroupPw              = Play.current.configuration.getString(Constants.JunGroup.Pw).get
  private lazy final val JunGroupEmail           = Play.current.configuration.getString(Constants.JunGroup.Email).get

  private lazy final val AuthFailureDelayMs      = Play.current.configuration.getLong(Constants.AppConfig.AuthFailureDelayMs).get
  private lazy final val OpsEmail                = Play.current.configuration.getString(Constants.AppConfig.OpsEmail).get
  private lazy final val TeamEmail               = Play.current.configuration.getString(Constants.AppConfig.TeamEmail).get
  private lazy final val HttpAuthUser            = Play.current.configuration.getString(Constants.AppConfig.HttpAuthUser).get
  private lazy final val HttpAuthPw              = Play.current.configuration.getString(Constants.AppConfig.HttpAuthPw).get
  private lazy final val WebDriverType           = Play.current.configuration.getString(Constants.AppConfig.WebDriverType).get

  private lazy final val ApplovinUrl             = Play.current.configuration.getString(Constants.Reporting.ApplovinUrl).get
  private lazy final val HyprMarketplaceUrl      = Play.current.configuration.getString(Constants.Reporting.HyprMarketplaceUrl).get
  private lazy final val AdcolonyUrl             = Play.current.configuration.getString(Constants.Reporting.AdcolonyUrl).get
  private lazy final val VungleUrl               = Play.current.configuration.getString(Constants.Reporting.VungleUrl).get
  private lazy final val UnityadsUrl             = Play.current.configuration.getString(Constants.Reporting.UnityadsUrl).get

  private lazy final val KeenOrgId               = Play.current.configuration.getString(Constants.KeenConfig.OrgID).get
  private lazy final val KeenOrgKey              = Play.current.configuration.getString(Constants.KeenConfig.OrgKey).get
  private lazy final val ErrorProjectID          = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectID).get
  private lazy final val ErrorProjectKey         = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectKey).get
  private lazy final val ReviewErrorProjectID    = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectID).get
  private lazy final val ReviewErrorProjectKey   = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectKey).get

  private lazy final val SignerAlgorithm         = Play.current.configuration.getString(Constants.Hmac.SignerAlgorithm).get

  // these change for review apps
  private lazy final val AppDomain               = Play.current.configuration.getString(Constants.AppConfig.AppDomain).get
  private lazy final val IosDomain               = Play.current.configuration.getString(Constants.AppConfig.S2sIosCallbackDomain).get
  private lazy final val AndroidDomain           = Play.current.configuration.getString(Constants.AppConfig.S2sAndroidCallbackDomain).get
  private lazy final val PlayerCallbackUrl       = Play.current.configuration.getString(Constants.AppConfig.PlayerCallbackUrl).get

  private lazy final val IosAdProviderID         = Play.current.configuration.getLong(Constants.AdProviderConfig.IosAdProviderID).get
  private lazy final val AndroidAdProviderID     = Play.current.configuration.getLong(Constants.AdProviderConfig.AndroidAdProviderID).get

  private lazy final val KeenProject             = Play.current.configuration.getString(s"${Constants.KeenPrefix}.${Constants.KeenConfig.Project}").get
  private lazy final val KeenWriteKey            = Play.current.configuration.getString(s"${Constants.KeenPrefix}.${Constants.KeenConfig.WriteKey}").get
  private lazy final val KeenReadKey             = Play.current.configuration.getString(s"${Constants.KeenPrefix}.${Constants.KeenConfig.ReadKey}").get
  private lazy final val KeenMasterKey           = Play.current.configuration.getString(s"${Constants.KeenPrefix}.${Constants.KeenConfig.MasterKey}").get

  //@formatter:on

  object ConfigVarsHmac {
    val (algorithm: String) = {
      (SignerAlgorithm)
    }

    override def toString = {
      s"""
         |HMAC CONFIG
         |ALGORITHM:    $algorithm
       """.stripMargin
    }
  }

  object ConfigVarsCallbackUrls {
    val (ios: String,
    android: String,
    player: String) = {
      if (Environment.isReviewApp) {
        (s"https://$AppName.herokuapp.com",
          s"https://$AppName.herokuapp.com", {
          val url = PlayerCallbackUrl.replace("https://callback.hyprmx.com", s"https://$AppName.herokuapp.com")
          if (url.startsWith(s"https://$AppName.herokuapp.com")) {
            url
          } else {
            PlayerCallbackUrl.replace(ParentName, AppName)
          }
        })
      } else {
        (IosDomain,
          AndroidDomain,
          PlayerCallbackUrl)
      }
    }

    override def toString = {
      s"""
         |CALLBACK CONFIG
         |IOS:        $ios
         |ANDROID:    $android
         |PLAYER:     $player
       """.stripMargin
    }
  }

  object ConfigVarsAdProviders {
    val (iosID: Long,
    androidID: Long) =
      (IosAdProviderID, AndroidAdProviderID)

    override def toString = {
      s"""
         |ADPROVIDER CONFIG
         |IOS ID:       $iosID
         |ANDROID ID:   $androidID
       """.stripMargin
    }
  }

  object ConfigVarsHeroku {
    val (latestSha: String,
    latestBranch: String) =
      (LatestSha, LatestBranch)

    override def toString = {
      s"""
         |HEROKU CONFIG
         |LATEST SHA:       $latestSha
         |LATEST BRANCH:    $latestBranch
       """.stripMargin
    }
  }

  object ConfigVarsReporting {
    val (applovinUrl: String,
    hyprMarketplaceUrl: String,
    adcolonyUrl: String,
    vungleUrl: String,
    unityadsUrl: String) =
      (ApplovinUrl,
        HyprMarketplaceUrl,
        AdcolonyUrl,
        VungleUrl,
        UnityadsUrl)

    override def toString = {
      s"""
         |REPORTING CONFIG
         |APPLOVIN URL:         $applovinUrl
         |HYPRMARKETPLACE URL:  $hyprMarketplaceUrl
         |ADCOLONY URL:         $adcolonyUrl
         |VUNGLE URL:           $vungleUrl
         |UNITYADS URL:         $unityadsUrl
       """.stripMargin
    }
  }

  object ConfigVarsJunGroup {
    val (url: String,
    token: String,
    user: String,
    pw: String,
    email: String) = {
      (JunGroupUrl,
        JunGroupToken,
        JunGroupUser,
        JunGroupPw,
        JunGroupEmail)
    }

    override def toString = {
      s"""
         |JUNGROUP CONFIG
         |JUNGROUP URL:     $url
         |JUNGROUP TOKEN:   $token
         |JUNGROUP USER:    $user
         |JUNGROUP PW:      $pw
         |JUNGROUP EMAIL:   $email
       """.stripMargin
    }
  }

  object ConfigVarsApp {
    val (staging: String,
    appName: String,
    parentName: String,
    domain: String,
    httpAuthUser: String,
    httpAuthPw: String,
    teamEmail: String,
    opsEmail: String,
    authFailureDelayMs: Long,
    webDriverType: String) = {
      if (Environment.isReviewApp) {
        (Staging,
          AppName,
          ParentName,
          s"https://$AppName.herokuapp.com",
          HttpAuthUser,
          HttpAuthPw,
          TeamEmail,
          OpsEmail,
          AuthFailureDelayMs,
          WebDriverType)
      } else {
        (Staging,
          AppName,
          ParentName,
          AppDomain,
          HttpAuthUser,
          HttpAuthPw,
          TeamEmail,
          OpsEmail,
          AuthFailureDelayMs,
          WebDriverType)
      }
    }

    override def toString = {
      s"""
         |APPLICATION CONFIG
         |STAGING:                $staging
         |APP NAME:               $appName
         |PARENT NAME:            $parentName
         |DOMAIN:                 $domain
         |AUTH USER:              $httpAuthUser
         |AUTH PW:                $httpAuthPw
         |TEAM EMAIL:             $teamEmail
         |OPS EMAIL:              $opsEmail
         |AUTH FAILURE DELAY MS:  $authFailureDelayMs
         |WEB DRIVER:             $webDriverType
       """.stripMargin
    }
  }

  // Will be an empty Map if something is wrong, otherwise set depending on whether this is a review app or not
  object ConfigVarsKeen {
    val (error: Option[Any],
    projectID: String,
    writeKey: String,
    readKey: String,
    masterKey: String,
    errorProjectID: String,
    errorProjectKey: String,
    orgID: String,
    orgKey: String) = {
      Environment.isReviewApp && KeenOrgId != "" && KeenOrgKey != "" match {
        case true =>
          val res = new ReviewApp().createOrGetKeenProject(ConfigVarsApp.appName, OpsEmail, KeenOrgId, KeenOrgKey)
          val error = res.get("error")
          val result = if (error.isEmpty) {
            (None,
              res.get(Constants.KeenConfig.ProjectID).get,
              res.get(Constants.KeenConfig.WriteKey).get,
              res.get(Constants.KeenConfig.ReadKey).get,
              res.get(Constants.KeenConfig.MasterKey).get,
              ReviewErrorProjectID,
              ReviewErrorProjectKey,
              KeenOrgId,
              KeenOrgKey)
          } else {
            Logger.error(s"Creating review app Keen project had an error: ${error.get.toString}")
            (error,
              KeenProject,
              KeenWriteKey,
              KeenReadKey,
              KeenMasterKey,
              ReviewErrorProjectID,
              ReviewErrorProjectKey,
              KeenOrgId,
              KeenOrgKey)
          }
          result

        case _ =>
          Environment.isReviewApp match {
            case true =>
              val err = s"This review app: $AppName  does not have usable Keen organization environ vars "
              Logger.error(s"ERROR: This review app: $AppName  does not have usable Keen organization environ vars Keen Organization ID: $KeenOrgId   KEY: $KeenOrgKey")
              (Some(err),
                KeenProject,
                KeenWriteKey,
                KeenReadKey,
                KeenMasterKey,
                ErrorProjectID,
                ErrorProjectKey,
                KeenOrgId,
                KeenOrgKey)

            case _ =>
              (None,
                KeenProject,
                KeenWriteKey,
                KeenReadKey,
                KeenMasterKey,
                ErrorProjectID,
                ErrorProjectKey,
                KeenOrgId,
                KeenOrgKey)
          }
      }
    }

    override def toString = {
      s"""
         |KEEN CONFIG
         |PROJECT :           $projectID
         |WRITE KEY:          $writeKey
         |READ KEY:           $readKey
         |MASTER KEY:         $masterKey
         |ERROR PROJECT ID:   $errorProjectID
         |ERROR PROJECT KEY:  $errorProjectKey
         |ORG ID:             $orgID
         |ORG KEY:            $orgKey
       """.stripMargin
    }
  }

  def dumpKeen = s"Keen: project ${ConfigVarsKeen.projectID} writeKey ${ConfigVarsKeen.writeKey}"
}
