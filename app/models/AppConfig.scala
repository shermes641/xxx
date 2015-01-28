package models

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import models.Waterfall.AdProviderInfo
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.language.postfixOps

/**
 * Encapsulates configuration data for Waterfalls, WaterfallAdProviders, Apps, and VirtualCurrencies along with a generation number.
 * @param generationNumber A number indicating how many times the AppConfig configuration has been edited.
 * @param configuration The JSON API response expected by the SDK.
 */
case class AppConfig(generationNumber: Long, configuration: JsValue)

object AppConfig extends JsonConversion {
  val TEST_MODE_DISTRIBUTOR_ID = "111"
  val TEST_MODE_PROVIDER_NAME = "HyprMarketplace"
  val TEST_MODE_PROVIDER_ID = 0.toLong
  val TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_NAME = "Test Distributor"
  val TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_ID = 0.toLong
  val TEST_MODE_HYPRMEDIATE_APP_ID = 0.toLong
  val TEST_MODE_APP_NAME = "Test App"
  val TEST_MODE_PROPERTY_ID = " "
  val TEST_MODE_VIRTUAL_CURRENCY = new VirtualCurrency(0, 0, "Coins", 100, 1, Some(100), true)
  val TEST_MODE_APP_CONFIG_REFRESH_INTERVAL = 0

  /**
   * Determines if the latest configuration in the database differs from the new configuration about to be saved.
   * @param oldConfiguration The configuration currently persisted in the database.
   * @param newConfiguration The new configuration about to be saved.
   * @return A Boolean value, indicating if the two configurations are different.
   */
  def configurationDiffers(oldConfiguration: JsValue, newConfiguration: JsValue): Boolean = {
    val configurations = List(oldConfiguration, newConfiguration).map { configuration =>
      configuration.as[JsObject].fields.filterNot(element => element._1 == "generationNumber")
    }
    configurations(0) != configurations(1)
  }

  /**
   * Creates a new AppConfig record in the app_configs table.
   * @param appID The ID of the App to which the AppConfig belongs.
   * @param appToken The unique identifier for an App.
   * @param connection DB connection for transaction.
   * @return The new generation number if the insert is successful; otherwise, returns the original generation number argument.
   */
  def create(appID: Long, appToken: String, generationNumber: Long)(implicit connection: Connection): Option[Long] = {
    val currentConfiguration = responseV1(appToken)
    findLatestWithTransaction(appToken) match {
      case Some(latestGeneration) if(generationNumber != latestGeneration.generationNumber) => {
        throw new IllegalArgumentException
      }
      case Some(latestGeneration) if(configurationDiffers(latestGeneration.configuration, currentConfiguration) && generationNumber == latestGeneration.generationNumber) => {
        val newGenerationNumber = generationNumber + 1
        val configurationWithGeneration: JsObject = currentConfiguration.as[JsObject].deepMerge(JsObject(Seq("generationNumber" -> JsNumber(newGenerationNumber))))
        val result = insert(appID, appToken, configurationWithGeneration, generationNumber + 1).executeInsert()
        result match {
          case Some(result: Long) => Some(newGenerationNumber)
          case _ => Some(generationNumber)
        }
      }
      case None => {
        val configurationWithGeneration: JsObject = currentConfiguration.as[JsObject].deepMerge(JsObject(Seq("generationNumber" -> JsNumber(generationNumber))))
        insert(appID, appToken, configurationWithGeneration, generationNumber).executeInsert()
        Some(generationNumber)
      }
      case _ => Some(generationNumber)
    }
  }

  /**
   * SQL statement for inserting a new record into the app_configs table.
   * @param appID The ID of the App to which the AppConfig belongs.
   * @param appToken The unique identifier for an App.
   * @param currentConfiguration The JSON configuration of ad providers used in the APIController.
   * @return A SQL statement to be executed by the create method.
   */
  def insert(appID: Long, appToken: String, currentConfiguration: JsValue, generationNumber: Long): SimpleSql[Row] = {
    SQL(
      """
        INSERT INTO app_configs (generation_number, app_id, app_token, configuration)
        VALUES ({generation_number}, {app_id}, {app_token}, CAST({configuration} AS json));
      """
    ).on("app_id" -> appID, "app_token" -> appToken, "configuration" -> Json.stringify(currentConfiguration), "generation_number" -> generationNumber)
  }

  /**
   * Creates an AppConfig, within a transaction, using the Waterfall ID.
   * @param waterfallID The ID of the Waterfall to which the AppConfig belongs.
   * @param currentGenerationNumber A number tracking the current state of the AppConfig for this particular Waterfall.
   * @return If successful, ID of the AppConfig; otherwise, None.
   */
  def createWithWaterfallIDInTransaction(waterfallID: Long, currentGenerationNumber: Option[Long])(implicit connection: Connection): Option[Long] = {
    App.findByWaterfallID(waterfallID) match {
      case Some(app) => {
        val generationNumber = currentGenerationNumber match {
          case Some(number) => number
          case None => {
            findLatestWithTransaction(app.token) match {
              case Some(config) => config.generationNumber
              case None => 0
            }
          }
        }
        create(app.id, app.token, generationNumber)
      }
      case None => None
    }
  }

  /**
   * A SQL statement for selecting the latest app_config record for a given App token.
   * @param appToken The token of the App to which the AppConfig belongs.
   * @return SQL to be executed by findLatest and findLatestWithTransaction methods.
   */
  def findLatestSQL(appToken: String): SimpleSql[Row] = {
    SQL(
      """
        SELECT generation_number, configuration FROM app_configs
        WHERE app_token={app_token}
        ORDER BY generation_number DESC LIMIT 1
        FOR UPDATE
      """
    ).on("app_token" -> appToken)
  }

  /**
   * Retrieves the latest configuration JSON for a given  token.
   * @param appToken The identifier used to find a particular AppConfig response.
   * @return An instance of the AppConfig class if one is found; otherwise, None.
   */
  def findLatest(appToken: String): Option[AppConfig] = {
    DB.withConnection { implicit connection =>
      findLatestSQL(appToken).as(appConfigParser*) match {
        case List(appConfig) => Some(appConfig)
        case _ => None
      }
    }
  }

  /**
   * Retrieves the latest configuration JSON, within a transaction, for a given App token.
   * @param appToken The identifier used to find a particular AppConfig response.
   * @return An instance of the AppConfig class if one is found; otherwise, None.
   */
  def findLatestWithTransaction(appToken: String)(implicit connection: Connection): Option[AppConfig] = {
    findLatestSQL(appToken).as(appConfigParser*) match {
      case List(appConfig) => Some(appConfig)
      case _ => None
    }
  }

  // Used to convert SQL row into an instance of the AppConfig class.
  val appConfigParser: RowParser[AppConfig] = {
    get[Long]("generation_number") ~
    get[JsValue]("configuration") map {
      case generation_number ~ configuration => AppConfig(generation_number, configuration)
    }
  }

  /**
   * Creates the Waterfall order JSON used in the APIController.
   * @param appToken The unique identifier used to look up the AppConfig .
   * @return A JSON object containing either ordered ad provider configurations or an error message.
   */
  def responseV1(appToken: String)(implicit connection: Connection): JsValue = {
    // Removes ad providers that are inactive or do not have a high enough eCPM value from the response.
    def filteredAdProviders(unfilteredAdProviders: List[AdProviderInfo]): List[AdProviderInfo] = {
      unfilteredAdProviders.filter(adProvider => adProvider.active.get && adProvider.meetsRewardThreshold)
    }
    Waterfall.order(appToken) match {
      // App token was not found in app_configs table.
      case adProviders: List[AdProviderInfo] if(adProviders.size == 0) => {
        Json.obj("status" -> "error", "message" -> "App Configuration not found.")
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
        JsonBuilder.appConfigResponseV1(providerList)
      }
      // All other cases.
      case adProviders: List[AdProviderInfo] => {
        JsonBuilder.appConfigResponseV1(filteredAdProviders(adProviders))
      }
    }
  }

  /**
   * Returns the JSON for an AppConfig when a Waterfall is in test mode.
   * @return A JSON object containing the test mode AppConfig info.
   */
  def testResponseV1: JsValue = {
    val testConfigData: JsValue = JsObject(Seq("requiredParams" -> JsObject(Seq("distributorID" -> JsString(TEST_MODE_DISTRIBUTOR_ID), "propertyID" -> JsString(TEST_MODE_PROPERTY_ID)))))
    val testAdProviderConfig: AdProviderInfo = new AdProviderInfo(Some(TEST_MODE_PROVIDER_NAME), Some(TEST_MODE_PROVIDER_ID), Some(TEST_MODE_APP_NAME), Some(TEST_MODE_HYPRMEDIATE_APP_ID),
      TEST_MODE_APP_CONFIG_REFRESH_INTERVAL, Some(TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_NAME), Some(TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_ID), Some(testConfigData), Some(5.0), Some(TEST_MODE_VIRTUAL_CURRENCY.name),
      Some(TEST_MODE_VIRTUAL_CURRENCY.exchangeRate), TEST_MODE_VIRTUAL_CURRENCY.rewardMin, TEST_MODE_VIRTUAL_CURRENCY.rewardMax, Some(TEST_MODE_VIRTUAL_CURRENCY.roundUp), true, false, Some(false))
    JsonBuilder.appConfigResponseV1(List(testAdProviderConfig)).as[JsObject].deepMerge(JsObject(Seq("testMode" -> JsBoolean(true))))
  }
}
