mediationModule.controller( 'WaterfallController', [ '$scope', '$http', '$routeParams', '$filter', '$timeout', '$location', 'flashMessage', 'sharedIDs',
        function( $scope, $http, $routeParams, $filter, $timeout, $location, flashMessage, sharedIDs ) {
            // Angular Templates
            $scope.appList = 'assets/templates/waterfalls/appList.html';
            $scope.subHeader = 'assets/templates/sub_header.html';
            $scope.editAppModal = 'assets/templates/apps/editAppModal.html';
            $scope.newAppModal = 'assets/templates/apps/newAppModal.html';
            $scope.editWaterfallAdProviderModal = 'assets/templates/waterfall_ad_providers/edit.html';
            $scope.testModeConfirmationModal = 'assets/templates/waterfalls/test_mode_confirmation.html';

            $scope.page = 'waterfall';
            $scope.newAppModalTitle = "Create New App";
            $scope.modalShown = false;
            $scope.showWaterfallAdProviderModal = false;
            $scope.adProviderModalShown = false;
            $scope.showTestModeConfirmationModal = false;
            $scope.showCodeBlock = false;
            $scope.disableTestModeToggle = false;
            $scope.waterfallInfoCallComplete = false;
            $scope.systemMessage = "";
            $scope.messages = [];
            $scope.errors = {};
            $scope.form = {};
            $scope.flashMessage = flashMessage;

            // Retrieve Waterfall data
            $scope.getWaterfallData = function() {
                $scope.waterfallInfoCallComplete = false;
                $http.get('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID + '/waterfall_info').success(function(data) {
                    $scope.waterfallData = data;
                    $scope.appName = data.waterfall.appName;
                    $scope.appID = data.waterfall.appID;
                    sharedIDs.setAppID($scope.appID);
                    $scope.distributorID = $routeParams.distributorID;
                    sharedIDs.setDistributorID($scope.distributorID);
                    $scope.generationNumber = data.generationNumber;
                    $scope.appToken = data.waterfall.appToken;
                    $scope.sortableOptions.disabled = $scope.waterfallData.waterfall.optimizedOrder;
                    $scope.sortableOptions.containment = "#waterfall-edit";
                    $scope.waterfallInfoCallComplete = true;
                }).error(function(data) {
                    $scope.waterfallInfoCallComplete = true;
                    flashMessage.add(data);
                });
            };

            $scope.getWaterfallData();

            // Callback for when the Waterfall order changes via the drag and drop
            $scope.sortableOptions = {
                stop: function(e, ui) {
                    $scope.setWaterfallOrder();
                    $scope.updateWaterfall();
                }
            };

            $scope.toggleCodeBlock = function() {
                $scope.showCodeBlock = !$scope.showCodeBlock;
            };

            $scope.confirmTestMode = function() {
                ga('send', 'event', 'testmode_toggle_confirm', 'click', 'waterfalls');
                $scope.waterfallData.waterfall.testMode = true;
                $scope.waterfallData.waterfall.paused = false;
                $scope.updateWaterfall();
                closeTestModeModal();
            };

            $scope.cancelTestMode = function() {
                ga('send', 'event', 'testmode_toggle_cancel', 'click', 'waterfalls');
                $scope.waterfallData.waterfall.testMode = false;
                closeTestModeModal();
            };

            var closeTestModeModal = function() {
                ga('send', 'event', 'testmode_toggle_close', 'click', 'waterfalls');
                $scope.showModal(false);
                $scope.showTestModeConfirmationModal = false;
            };

            $scope.activateTestMode = function() {
                $scope.showTestModeConfirmationModal = true;
                $scope.showModal(!$scope.modalShown);
            };

            $scope.activateLiveMode = function() {
                $scope.waterfallData.waterfall.paused = false;
                $scope.waterfallData.waterfall.testMode = false;
                $scope.updateWaterfall();
            };

            $scope.activatePausedMode = function() {
                $scope.waterfallData.waterfall.paused = true;
                $scope.waterfallData.waterfall.testMode = false;
                $scope.updateWaterfall();
            };

            // Toggles optimized mode on/off
            $scope.toggleOptimizedMode = function() {
                ga('send', 'event', 'optimized_toggle', 'click', 'waterfalls');
                $scope.sortableOptions.disabled = !$scope.sortableOptions.disabled;
                if($scope.waterfallData.waterfall.optimizedOrder) {
                    $scope.orderOptimizedWaterfallList();
                }
                $scope.updateWaterfall();
            };

            /* Waterfall logic */
            // Sets the waterfallOrder property for each WaterfallAdProvider.
            $scope.setWaterfallOrder = function() {
                for (var index in $scope.waterfallData.waterfallAdProviderList) {
                    $scope.waterfallData.waterfallAdProviderList[index].waterfallOrder = parseInt(index);
                }
            };

            // Sets the order of the Waterfall.
            $scope.orderOptimizedWaterfallList = function() {
                if($scope.waterfallData.waterfall.optimizedOrder) {
                    var newOrder = $scope.providersByActive(true).sort(function(li1, li2) {
                        return (Number(li2.cpm) - Number(li1.cpm))
                    });
                    var inactive = $scope.providersByActive(false);
                    newOrder.push.apply(newOrder, inactive);
                    $scope.waterfallData.waterfallAdProviderList = newOrder;
                    $scope.setWaterfallOrder();
                }
            };

            // Retrieves list of ad providers who are either active or inactive
            $scope.providersByActive = function(active) {
                return $scope.waterfallData.waterfallAdProviderList.filter(function(li) { return(li.active === active) });
            };

            // Submit updates to Waterfall
            $scope.updateWaterfall = function() {
                ga('send', 'event', 'waterfall_update', 'click');
                $scope.setWaterfallOrder();
                var params = {
                    adProviderOrder: $scope.waterfallData.waterfallAdProviderList.filter(function(el) { return(!el.newRecord); }),
                    optimizedOrder: $scope.waterfallData.waterfall.optimizedOrder,
                    testMode: $scope.waterfallData.waterfall.testMode,
                    paused: $scope.waterfallData.waterfall.paused,
                    appToken: $scope.appToken,
                    generationNumber: $scope.generationNumber
                };
                $http.post('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID, params).success(function(data) {
                    $scope.generationNumber = data.newGenerationNumber;
                    flashMessage.add(data);
                }).error(function(data) {
                    flashMessage.add(data);
                });
            };

            // Toggles active/inactive status for an AdProvider
            $scope.toggleWAPStatus = function(adProviderConfig) {
                ga('send', 'event', 'toggle_wap_status', 'click', 'waterfalls');

                adProviderConfig.active = !adProviderConfig.active;
                adProviderConfig.loading = true;
                $scope.updateWaterfall();
                $scope.orderOptimizedWaterfallList();

                // Delays the hover animation after toggling.  This is for UX purposes only.
                $timeout(function(){
                    adProviderConfig.loading = false;
                }, 2000);

            };

            /* App logic */
            // Open the App settings page
            $scope.toggleEditAppModal = function(appID) {
                ga('send', 'event', 'toggle_edit_app_modal', 'click', 'waterfalls');
                // Retrieve App data
                $http.get('/distributors/' + $routeParams.distributorID + '/apps/' + appID + '/edit').success(function(data) {
                    $scope.editAppID = appID;
                    $scope.data = data;
                    $scope.form.editAppForm.$setPristine();
                    $scope.form.editAppForm.$setUntouched();
                }).error(function(data) {
                    flashMessage.add(data);
                });

                $scope.showEditAppModal = !$scope.showEditAppModal;
                $scope.showModal(!$scope.modalShown);
            };

            // Open the App creation page
            $scope.toggleNewAppModal = function() {
                ga('send', 'event', 'toggle_new_app_modal', 'click', 'waterfalls');
                $scope.errors = {};
                $scope.newApp = {appName: null, currencyName: null, exchangeRate: null, rewardMin: null, rewardMax: null, roundUp: true};
                $scope.showNewAppModal = !$scope.showNewAppModal;
                $scope.form.newAppForm.$setPristine();
                $scope.form.newAppForm.$setUntouched();
                $scope.showModal(!$scope.modalShown);
            };

            // Submit form if fields are valid.
            $scope.submitNewApp = function(form) {
                if(form.$valid) {
                    form.submitting = true;
                    ga('send', 'event', 'submit_new_app', 'click', 'waterfalls');
                    var distributorID = $routeParams.distributorID;
                    setNumberValues("newApp");
                    $http.post('/distributors/' + distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
                            form.submitting = false;
                            $scope.toggleNewAppModal();
                            $location.path('/distributors/' + distributorID + '/waterfalls/' + data.waterfallID + '/edit').replace();
                            flashMessage.add(data);
                        }).error(function(data, status, headers, config) {
                            form.submitting = false;
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };

            var setNumberValues = function(scopeObject) {
                var parsedRewardMax = parseInt($scope[scopeObject].rewardMax);
                $scope[scopeObject].rewardMax = isNaN(parsedRewardMax) ? null : parsedRewardMax;
                $scope[scopeObject].rewardMin = parseInt($scope[scopeObject].rewardMin);
                $scope[scopeObject].exchangeRate = parseInt($scope[scopeObject].exchangeRate);
            };

            // Submit updates to App
            $scope.submitEditApp = function(form) {
                if(form.$valid) {
                    ga('send', 'event', 'submit_edit_app', 'click', 'waterfalls');
                    setNumberValues("data");
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.editAppID, $scope.data).
                        success(function(data, status, headers, config) {
                            var apps = $scope.waterfallData.appsWithWaterfalls;
                            if($scope.appName !== $scope.data.appName && $scope.appID === $scope.editAppID) {
                                $scope.appName = $scope.data.appName;
                            }
                            for(index in apps) {
                                if(apps[index].id === $scope.editAppID) {
                                    apps[index].name = $scope.data.appName;
                                }
                            }
                            $scope.showEditAppModal = false;
                            $scope.showModal(false);
                            flashMessage.add(data);
                        }).error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            } else {
                                flashMessage.add(data);
                            }
                        });
                }
            };

            /* WaterfallAdProvider logic */
            // Sets WaterfallAdProvider data on a successful response from the server
            var setWAPData = function(wapData) {
                $scope.wapData = wapData;
                $scope.wapData.cpm = $filter("monetaryFormat")(wapData.cpm);
                $scope.form.editWAP.$setPristine();
                $scope.form.editWAP.$setUntouched();
                $scope.showWaterfallAdProviderModal = true;
                $scope.showModal(true);
                for(var i = 0; i < wapData.requiredParams.length; i++) {
                    var param = wapData.requiredParams[i];
                    $scope.restartableParams[param.displayKey] = param.value;
                }
            };

            // Retrieves WaterfallAdProvider data from the server if an instance exists.  Otherwise, create a new WaterfallAdProvider.
            $scope.editWaterfallAdProvider = function(adProviderConfig) {
                ga('send', 'event', 'edit_waterfall_ad_provider', 'click', 'waterfalls');
                $scope.restartableParams = {};
                $scope.changedRestartParams = {};
                $scope.errors = {};
                $scope.invalidForm = false;
                if(adProviderConfig.newRecord) {
                    var params = {
                        waterfallID: $routeParams.waterfallID,
                        appToken: $scope.appToken,
                        generationNumber: $scope.generationNumber,
                        configurable: adProviderConfig.configurable,
                        adProviderID: adProviderConfig.waterfallAdProviderID,
                        cpm: adProviderConfig.cpm
                    };
                    // Create a new WaterfallAdProvider
                    $http.post("/distributors/" + $scope.distributorID + "/waterfall_ad_providers", params).success(function(data) {
                        for(var i = 0; i < $scope.waterfallData.waterfallAdProviderList.length; i++) {
                            var provider = $scope.waterfallData.waterfallAdProviderList[i];
                            if(provider.waterfallAdProviderID === params["adProviderID"]) {
                                provider.newRecord = false;
                                provider.waterfallAdProviderID = data.wapID;
                            }
                        }
                        $scope.generationNumber = data.newGenerationNumber;
                        setWAPData(data)
                    }).error(function(data) {
                        flashMessage.add(data);
                    });
                } else {
                    // If a WaterfallAdProvider already exists, retrieve its data from the server
                    $http.get('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + adProviderConfig.waterfallAdProviderID + '/edit', {params: {app_token: $scope.appToken}}).success(function(wapData) {
                        setWAPData(wapData);
                    }).error(function(data) {
                        flashMessage.add(data);
                    });
                }
            };

            // Checks WaterfallAdProvider modal form and submits update if valid.
            $scope.updateWAP = function(form) {
                $scope.errors = {};
                // Check for modified params that require an App restart
                for (var i = 0; i < $scope.wapData.requiredParams.length; i++) {
                    var param = $scope.wapData.requiredParams[i];
                    if (param.displayKey != "" && param.value != $scope.restartableParams[param.displayKey]) {
                        $scope.changedRestartParams[param.displayKey] = param.value;
                    }
                }
                var parsedCpm = parseFloat($scope.wapData.cpm);
                if(form.$valid) {
                    ga('send', 'event', 'update_waterfall_ad_provider', 'click', 'waterfalls');
                    $scope.wapData.generationNumber = $scope.generationNumber;
                    $scope.wapData.appToken = $scope.appToken;
                    $scope.wapData.waterfallID = $routeParams.waterfallID;
                    // Submit update for WaterfallAdProvider
                    $http.post('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + $scope.wapData.waterfallAdProviderID, $scope.wapData).success(function(data) {
                        $scope.generationNumber = data.newGenerationNumber;
                        var adProviders = $scope.waterfallData.waterfallAdProviderList;
                        for(var i = 0; i < adProviders.length; i++) {
                            if(adProviders[i].name === $scope.wapData.adProviderName) {
                                if(adProviders[i].unconfigured) {
                                    $scope.changedRestartParams = {};
                                }
                                adProviders[i].cpm = parsedCpm;
                                adProviders[i].unconfigured = false;
                            }
                        }
                        $scope.orderOptimizedWaterfallList();
                        $scope.updateWaterfall();
                        $scope.showWaterfallAdProviderModal = false;
                        $scope.showModal(false);
                        var restartParams = Object.keys($scope.changedRestartParams);
                        var successMessage = $scope.wapData.adProviderName + " updated!";
                        flashMessage.add({message: generateWAPSuccessMesage(successMessage, restartParams), status: "success"});
                    }).error(function(data) {
                        flashMessage.add(data);
                    });
                }
            };

            // If necessary, adds param names which require an App restart to the success message
            var generateWAPSuccessMesage = function(message, restartParams) {
                if(restartParams.length > 0) {
                    var paramMessage = restartParams.length == 1 ? restartParams[0] : restartParams.slice(0, restartParams.length - 1).join(", ") + ", and " + restartParams[restartParams.length - 1];
                    message += " Changes to " + paramMessage + " will require your app to be restarted to take effect.";
                }
                return message;
            };
        } ]
);
