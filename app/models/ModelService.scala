package models

import javax.inject._
import play.api.Configuration

/**
  * Helper class to encapsulate service classes
  * @param distributorUserService        The dependency-injected instance of DistributorUserService
  * @param distributorService            The dependency-injected instance of DistributorService
  * @param appService                    The dependency-injected instance of AppService
  * @param adProviderService             The dependency-injected instance of AdProviderService
  * @param waterfallService              The dependency-injected instance of WaterfallService
  * @param waterfallAdProviderService    The dependency-injected instance of WaterfallAdProviderService
  * @param appConfigServiceService       The dependency-injected instance of AppConfigService
  * @param virtualCurrencyServiceService The dependency-injected instance of VirtualCurrencyService
  * @param jsonBuilder                   The dependency-injected instance of JsonBuilderService
  * @param platform                      The dependency-injected instance of the Platform class
  * @param environmentConfig             The dependency-injected instance of Play's configuration
  */
@Inject
case class ModelService @Inject() (distributorUserService: DistributorUserService,
                                   distributorService: DistributorService,
                                   appService: AppService,
                                   adProviderService: AdProviderService,
                                   waterfallService: WaterfallService,
                                   waterfallAdProviderService: WaterfallAdProviderService,
                                   appConfigServiceService: AppConfigService,
                                   virtualCurrencyServiceService: VirtualCurrencyService,
                                   jsonBuilder: JsonBuilder,
                                   platform: Platform,
                                   environmentConfig: Configuration)


