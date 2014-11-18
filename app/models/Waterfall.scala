package models

import anorm._
import anorm.SqlParser._
import controllers.ConfigInfo
import java.sql.Connection
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import scala.language.postfixOps

case class Waterfall(id: Long, app_id: Long, name: String, token: String, optimizedOrder: Boolean, testMode: Boolean)

object Waterfall extends JsonConversion {
  val TEST_MODE_DISTRIBUTOR_ID = "111"
  val TEST_MODE_PROVIDER_NAME = "HyprMarketplace"
  val TEST_MODE_PROVIDER_ID = 0.toLong
  val TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_NAME = "Test Distributor"
  val TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_ID = 0.toLong
  val TEST_MODE_HYPRMEDIATE_APP_ID = 0.toLong
  val TEST_MODE_APP_NAME = "Test App"
  val TEST_MODE_APP_ID = " "
  val TEST_MODE_VIRTUAL_CURRENCY = new VirtualCurrency(0, 0, "Coins", 100, Some(1), Some(100), true)

  // Used to convert SQL row into an instance of the Waterfall class.
  val waterfallParser: RowParser[Waterfall] = {
    get[Long]("waterfalls.id") ~
    get[Long]("app_id") ~
    get[String]("name") ~
    get[String]("token") ~
    get[Boolean]("optimized_order") ~
    get[Boolean]("test_mode") map {
      case id ~ app_id ~ name  ~ token ~ optimized_order ~ test_mode => Waterfall(id, app_id, name, token, optimized_order, test_mode)
    }
  }

  /**
   * SQL statement for inserting a new record into the waterfalls table.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @param token A unique identifier used for API requests.
   * @return A SQL statement to be executed by create or createWithTransaction methods.
   */
  def insert(appID: Long, name: String, token: String): SimpleSql[Row] = {
    SQL(
      """
        INSERT INTO waterfalls (app_id, name, token)
        VALUES ({app_id}, {name}, {token});
      """
    ).on("app_id" -> appID, "name" -> name, "token" -> token)
  }

  /**
   * Creates a new Waterfall record in the database.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(appID: Long, name: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      val waterfallToken = generateToken
      val waterfallID = insert(appID, name, waterfallToken).executeInsert()
      waterfallID match {
        case Some(id: Long) => WaterfallGeneration.create(id, waterfallToken)
        case None => None
      }
      waterfallID
    }
  }

  /**
   * Executes SQL from insert method within a database transaction.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @param connection Database transaction
   * @return ID of new record if insert is successful, otherwise None.
   */
  def createWithTransaction(appID: Long, name: String)(implicit connection: Connection): Option[Long] = {
    val waterfallToken = generateToken
    val waterfallID = insert(appID, name, waterfallToken).executeInsert()
    waterfallID match {
      case Some(id: Long) => WaterfallGeneration.createWithTransaction(id, waterfallToken)
      case None => None
    }
    waterfallID
  }

  /**
   * Updates the fields for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param optimizedOrder Boolean value which determines if the waterfall should always be ordered by eCPM or not.
   * @param testMode Boolean value which determines if the waterfall is live or not.
   * @return Number of rows updated
   */
  def update(id: Long, optimizedOrder: Boolean, testMode: Boolean): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfalls
          SET optimized_order={optimized_order}, test_mode={test_mode}
          WHERE id={id};
        """
      ).on("optimized_order" -> optimizedOrder, "test_mode" -> testMode, "id" -> id).executeUpdate()
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param waterfallID ID of current Waterfall
   * @return Waterfall instance
   */
  def find(waterfallID: Long): Option[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*
          FROM waterfalls
          WHERE id={id};
        """
      ).on("id" -> waterfallID)
      query.as(waterfallParser*) match {
        case List(waterfall) => Some(waterfall)
        case _ => None
      }
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param appID ID of current App
   * @return List of Waterfall instances if query is successful.  Otherwise, returns an empty list.
   */
  def findByAppID(appID: Long): List[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*
          FROM waterfalls
          WHERE app_id={app_id};
        """
      ).on("app_id" -> appID)
      query.as(waterfallParser*).toList
    }
  }

  /**
   * Encapsulates info for App's server to server callback.
   * @param callbackURL The URL which receives the result of the completion attempt.
   * @param serverToServerEnabled Boolean value that determines if we should use the callbackURL.
   */
  case class WaterfallCallbackInfo(callbackURL: Option[String], serverToServerEnabled: Boolean)

  // Used to convert SQL row to an instance of WaterfallCallbackInfo class.
  val waterfallCallbackInfoParser: RowParser[WaterfallCallbackInfo] = {
    get[Option[String]]("callback_url") ~
    get[Boolean]("server_to_server_enabled") map {
      case callback_url ~ server_to_server_enabled => WaterfallCallbackInfo(callback_url, server_to_server_enabled)
    }
  }

  /**
   * Finds the callback information for an app based on the waterfall token.
   * @param waterfallToken Maps to the token field of the waterfalls table.
   * @return An instance of the WaterfallCallbackInfo if the token is found; otherwise, returns None.
   */
  def findCallbackInfo(waterfallToken: String): Option[WaterfallCallbackInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.callback_url, apps.server_to_server_enabled
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          WHERE waterfalls.token={waterfall_token};
        """
      ).on("waterfall_token" -> waterfallToken)
      query.as(waterfallCallbackInfoParser*) match {
        case List(waterfallCallbackInfo) => Some(waterfallCallbackInfo)
        case _ => None
      }
    }
  }

  /**
   * Retrieves the order of ad providers with their respective configuration data.
   * @param token API token used to authenticate a request and find a particular waterfall.
   * @return A list containing instances of AdProviderInfo if any active WaterfallAdProviders exist.
   */
  def order(token: String): List[AdProviderInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ap.name provider_name, ap.id as provider_id, apps.id as app_id, apps.name as app_name, distributors.id as distributor_id, distributors.name as distributor_name,
          wap.configuration_data, wap.cpm, vc.name as vc_name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up, w.test_mode, w.optimized_order, wap.active
          FROM waterfalls w
          FULL OUTER JOIN waterfall_ad_providers wap on wap.waterfall_id = w.id
          FULL OUTER JOIN ad_providers ap on ap.id = wap.ad_provider_id
          FULL OUTER JOIN virtual_currencies vc on vc.app_id = w.app_id
          FULL OUTER JOIN apps on apps.id = vc.app_id
          FULL OUTER JOIN distributors on apps.distributor_id = distributors.id
          WHERE w.token={token}
          ORDER BY wap.waterfall_order ASC
        """
      ).on("token" -> token)
      query.as(adProviderParser*).toList
    }
  }

  /**
   * Creates the Waterfall order JSON used in the APIController.
   * @param token The unique identifier used to look up information for a given Waterfall.
   * @return A JSON object containing either ordered ad provider configurations or an error message.
   */
  def responseV1(token: String): JsValue = {
    // Removes ad providers that are inactive or do not have a high enough eCPM value from the response.
    def filteredAdProviders(unfilteredAdProviders: List[AdProviderInfo]): List[AdProviderInfo] = {
      unfilteredAdProviders.filter(adProvider => adProvider.active.get && adProvider.meetsRewardThreshold)
    }
    Waterfall.order(token) match {
      // Token was not found in waterfalls table.
      case adProviders: List[AdProviderInfo] if(adProviders.size == 0) => {
        Json.obj("status" -> "error", "message" -> "Waterfall not found.")
      }
      // Waterfall is in test mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).testMode) => {
        testResponseV1
      }
      // No ad providers are active.
      case adProviders: List[AdProviderInfo] if(filteredAdProviders(adProviders).size == 0) => {
        Json.obj("status" -> "error", "message" -> "At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
      }
      // Waterfall is in "Optimized" mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).optimizedOrder) => {
        val providerList = filteredAdProviders(adProviders).sortWith { (provider1, provider2) =>
          (provider1.cpm, provider2.cpm) match {
            case (Some(cpm1: Double), Some(cpm2: Double)) => cpm1 > cpm2
            case (_, _) => false
          }
        }
        JsonBuilder.waterfallResponse(providerList)
      }
      // All other cases.
      case adProviders: List[AdProviderInfo] => {
        JsonBuilder.waterfallResponse(filteredAdProviders(adProviders))
      }
    }
  }

  def testResponseV1: JsValue = {
    val testConfigData: JsValue = JsObject(Seq("requiredParams" -> JsObject(Seq("distributorID" -> JsString(TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(TEST_MODE_APP_ID)))))
    val testAdProviderConfig: AdProviderInfo = new AdProviderInfo(Some(TEST_MODE_PROVIDER_NAME), Some(TEST_MODE_PROVIDER_ID), Some(TEST_MODE_APP_NAME), Some(TEST_MODE_HYPRMEDIATE_APP_ID),
      Some(TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_NAME), Some(TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_ID), Some(testConfigData), Some(5.0), Some(TEST_MODE_VIRTUAL_CURRENCY.name),
      Some(TEST_MODE_VIRTUAL_CURRENCY.exchangeRate), TEST_MODE_VIRTUAL_CURRENCY.rewardMin, TEST_MODE_VIRTUAL_CURRENCY.rewardMax, Some(TEST_MODE_VIRTUAL_CURRENCY.roundUp), true, false, Some(false))
    JsonBuilder.waterfallResponse(List(testAdProviderConfig))
  }

  // Used to convert SQL row into an instance of the AdProviderInfo class in Waterfall.order.
  val adProviderParser: RowParser[AdProviderInfo] = {
    get[Option[String]]("provider_name") ~
    get[Option[Long]]("provider_id") ~
    get[Option[String]]("app_name") ~
    get[Option[Long]]("app_id") ~
    get[Option[String]]("distributor_name") ~
    get[Option[Long]]("distributor_id") ~
    get[Option[JsValue]]("configuration_data") ~
    get[Option[Double]]("cpm") ~
    get[Option[String]]("vc_name") ~
    get[Option[Long]]("exchange_rate") ~
    get[Option[Long]]("reward_min") ~
    get[Option[Long]]("reward_max") ~
    get[Option[Boolean]]("round_up") ~
    get[Boolean]("test_mode") ~
    get[Boolean]("optimized_order") ~
    get[Option[Boolean]]("active") map {
      case provider_name ~ provider_id ~ app_name ~ app_id ~ distributor_name ~ distributor_id ~ configuration_data ~ cpm ~
           vc_name ~ exchange_rate ~ reward_min ~ reward_max ~ round_up ~ test_mode ~ optimized_order ~ active => {
        AdProviderInfo(provider_name, provider_id, app_name, app_id, distributor_name, distributor_id, configuration_data,
                       cpm, vc_name, exchange_rate, reward_min, reward_max, round_up, test_mode, optimized_order, active)
      }
    }
  }

  /**
   * Encapsulates necessary information returned from SQL query in Waterfall.order.
   * @param providerName Maps to the name field in the ad_providers table.
   * @param providerID Maps to the id field in the ad_providers table.
   * @param appName Maps to the name field in the apps table.
   * @param appID Maps to the id field in the apps table.
   * @param distributorName Maps to the name field in the distributors table.
   * @param distributorID Maps to the id field in the distributors table.
   * @param configurationData Maps to the configuration_data field in the waterfall_ad_providers table.
   * @param cpm Maps to the cpm field of waterfall_ad_providers table.
   * @param virtualCurrencyName Maps to the name field in the virtual_currencies table.
   * @param exchangeRate Maps to the exchange_rate field of the virtual_currencies table.
   * @param rewardMin Maps to the reward_min field of the virtual_currencies table.
   * @param rewardMax Maps to the reward_max field of the virtual_currencies table.
   * @param roundUp Maps to the round_up field of the virtual_currencies table.
   * @param testMode Determines if a waterfall is live or not.
   * @param optimizedOrder Determines if the waterfall_ad_providers should be sorted by cpm or not.
   * @param active Determines if a waterfall_ad_provider record should be included in the waterfall order.
   */
  case class AdProviderInfo(providerName: Option[String], providerID: Option[Long], appName: Option[String], appID: Option[Long], distributorName: Option[String], distributorID: Option[Long],
                            configurationData: Option[JsValue], cpm: Option[Double], virtualCurrencyName: Option[String], exchangeRate: Option[Long],
                            rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean], testMode: Boolean, optimizedOrder: Boolean, active: Option[Boolean]) {
    lazy val meetsRewardThreshold: Boolean = {
      (roundUp, cpm, rewardMin) match {
        case (Some(roundUpValue: Boolean), _, _) if(roundUpValue) => true
        case (Some(roundUpValue: Boolean), Some(cpmVal: Double), Some(minReward: Long)) if(!roundUpValue) => {
          val rewardAmount = minReward / exchangeRate.get
          (cpmVal / 1000) >= rewardAmount
        }
        // If there is no cpm value for an ad provider and the virtual currency does not roundUp, this will ensure it is excluded from the waterfall.
        case (Some(roundUpValue: Boolean), None, _) if(!roundUpValue) => false
        case (_, _, _) => true
      }
    }
  }

  /**
   * Updates WaterfallAdProvider records according to the configuration in the Waterfall edit view.
   * @param waterfallID ID of the Waterfall to which all WaterfallAdProviders belong.
   * @param adProviderConfigList List of attributes to update for each WaterfallAdProvider.
   * @return True if the update is successful; otherwise, false.
   */
  def reconfigureAdProviders(waterfallID: Long, adProviderConfigList: List[ConfigInfo]): Boolean = {
    var successful = true
    adProviderConfigList.map { adProviderConfig =>
      if(adProviderConfig.active && adProviderConfig.newRecord) {
        // If a Distributor wants to add a new AdProvider to the current waterfall, create a new WaterfallAdProvider record.
        WaterfallAdProvider.create(waterfallID, adProviderConfig.id, Some(adProviderConfig.waterfallOrder), adProviderConfig.cpm, adProviderConfig.configurable) match {
          case Some(id) => {}
          case None => successful = false
        }
      } else if(!adProviderConfig.newRecord) {
        //  Otherwise, find and update the existing WaterfallAdProvider record.
        WaterfallAdProvider.find(adProviderConfig.id) match {
          case Some(record) => {
            val newOrder = if(adProviderConfig.active) Some(adProviderConfig.waterfallOrder) else None
            val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, newOrder, record.cpm, Some(adProviderConfig.active), record.fillRate, record.configurationData, record.reportingActive)
            WaterfallAdProvider.update(updatedValues)
          }
          case _ => {
            successful = false
          }
        }
      }
    }
    successful
  }

  /**
   * Generates token field for waterfall.  This is called once on waterfall creation.
   * @return Random string to be saved as token.
   */
  def generateToken = {
    java.util.UUID.randomUUID.toString
  }
}
