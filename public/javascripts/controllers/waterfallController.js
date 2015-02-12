mediationModule.controller( 'WaterfallController', [ '$scope', '$http', '$routeParams', 'appCheck', '$filter', '$timeout', 'fieldsFilled',
        function( $scope, $http, $routeParams, appCheck, $filter, $timeout, fieldsFilled ) {
            // Angular Templates
            $scope.appList = 'assets/templates/waterfalls/appList.html';
            $scope.subHeader = 'assets/templates/sub_header.html';
            $scope.editAppModal = 'assets/templates/apps/editAppModal.html';
            $scope.newAppModal = 'assets/templates/apps/newAppModal.html';
            $scope.editWaterfallAdProviderModal = 'assets/templates/waterfall_ad_providers/edit.html';

            $scope.page = 'waterfall';
            $scope.newAppModalTitle = "Create New App";
            $scope.modalShown = false;
            $scope.showWaterfallAdProviderModal = false;
            $scope.adProviderModalShown = false;
            $scope.showCodeBlock = false;
            $scope.disableTestModeToggle = false;
            $scope.systemMessage = "";
            $scope.messages = [];

            // Retrieve Waterfall data
            $http.get('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID + '/waterfall_info').success(function(data) {
                $scope.waterfallData = data;
                $scope.appID = data.waterfall.appID;
                $scope.distributorID = $routeParams.distributorID;
                $scope.generationNumber = data.generationNumber;
                $scope.appToken = data.waterfall.appToken;
                $scope.disableTestModeToggle = checkTestModeToggle();
                $scope.sortableOptions.disabled = $scope.waterfallData.waterfall.optimizedOrder;
                $scope.sortableOptions.containment = "#waterfall-edit";
            }).error(function(data) {
                $scope.flashMessage(data);
            });

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

            // Checks status of WaterfallAdProviders to determine if the test mode toggle should be disabled
            var checkTestModeToggle = function() {
                var activeAdProviders = $scope.waterfallData.waterfallAdProviderList.filter(function(el, index) { return(el.active); });
                return (activeAdProviders.length < 1 && $scope.waterfallData.waterfall.testMode);
            };

            // Toggles test mode on/off
            $scope.toggleTestMode = function() {
                if(!$scope.disableTestModeToggle) {
                    $scope.updateWaterfall();
                    $scope.disableTestModeToggle = checkTestModeToggle();
                } else {
                    $scope.waterfallData.waterfall.testMode = !$scope.waterfallData.waterfall.testMode;
                    $scope.flashMessage({message: "You must activate at least one Ad Provider", status: "error"})
                }
            };

            // Toggles optimized mode on/off
            $scope.toggleOptimizedMode = function() {
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
                $scope.setWaterfallOrder();
                var params = {
                    adProviderOrder: $scope.waterfallData.waterfallAdProviderList.filter(function(el) { return(!el.newRecord); }),
                    optimizedOrder: $scope.waterfallData.waterfall.optimizedOrder,
                    testMode: $scope.waterfallData.waterfall.testMode,
                    appToken: $scope.appToken,
                    generationNumber: $scope.generationNumber
                };
                $http.post('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID, params).success(function(data) {
                    $scope.generationNumber = data.newGenerationNumber;
                    $scope.disableTestModeToggle = checkTestModeToggle();
                    $scope.flashMessage(data);
                }).error(function(data) {
                    $scope.flashMessage(data);
                });
            };

            // Toggles active/inactive status for an AdProvider
            $scope.toggleWAPStatus = function(adProviderConfig) {
                var activeAdProviders = $scope.waterfallData.waterfallAdProviderList.filter(function(el, index) { return(el.active); });
                var originalVal = adProviderConfig.active;
                // Only allow deactivation of Ad Provider if we are in Test mode or there is at least one other active Ad Provider.
                if(!originalVal || $scope.waterfallData.waterfall.testMode || (originalVal && (activeAdProviders.length > 1))) {
                    adProviderConfig.active = !adProviderConfig.active;
                    $scope.updateWaterfall();
                } else {
                    $scope.flashMessage({message: "At least one Ad Provider must be active", status: "error"})
                }
                $scope.disableTestModeToggle = checkTestModeToggle();
                $scope.orderOptimizedWaterfallList();
            };

            /* App logic */
            // Open the App settings page
            $scope.toggleEditAppModal = function() {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";

                // Retrieve App data
                $http.get('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID + '/edit').success(function(data) {
                    $scope.data = data;
                }).error(function(data) {
                    $scope.flashMessage(data);
                });

                $scope.showEditAppModal = !$scope.showEditAppModal;
                $scope.modalShown = !$scope.modalShown;
            };

            $scope.closeWAPModal = function() {
                $scope.modalShown = false;
                $scope.showWaterfallAdProviderModal = false;
            };

            // Open the App creation page
            $scope.toggleNewAppModal = function() {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
                $scope.newApp = {appName: null, currencyName: null, rewardMin: null, rewardMax: null, roundUp: true};
                $scope.showNewAppModal = !$scope.showNewAppModal;
                $scope.modalShown = !$scope.modalShown;
            };

            // Checks inputs for App creation page
            $scope.checkInputs = function(data) {
                var requiredFields = ['appName', 'currencyName', 'rewardMin', 'exchangeRate'];
                if(fieldsFilled(data, requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

            // Submit form if fields are valid.
            $scope.submitNewApp = function() {
                $scope.errors = {};
                var errorObjects = [appCheck.validRewardAmounts($scope.newApp), appCheck.validExchangeRate($scope.newApp.exchangeRate)];
                if(checkAppFormErrors($scope.newApp, errorObjects)) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
                            $scope.toggleNewAppModal();
                            $scope.flashMessage(data);
                        }).error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };

            var checkAppFormErrors = function(data, errorObjects) {
                for(var i = 0; i < errorObjects.length; i++) {
                    var error = errorObjects[i];
                    if(error.message) {
                        $scope.errors[error.fieldName] = error.message;
                        $scope.errors[error.fieldName + "Class"] = "error";
                        return false;
                    }
                }
                return true;
            };

            // Submit updates to App
            $scope.submitEditApp = function() {
                $scope.errors = {};
                var errorObjects = [appCheck.validRewardAmounts($scope.data), appCheck.validExchangeRate($scope.data.exchangeRate), appCheck.validCallback($scope.data)];
                if(checkAppFormErrors($scope.data, errorObjects)) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID, $scope.data).
                        success(function(data, status, headers, config) {
                            $scope.generationNumber = data.generationNumber;
                            $scope.showEditAppModal = false;
                            $scope.modalShown = false;
                            $scope.flashMessage(data);
                        }).error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };

            /* WaterfallAdProvider logic */
            // Sets WaterfallAdProvider data on a successful response from the server
            var setWAPData = function(wapData) {
                $scope.wapData = wapData;
                $scope.wapData.cpm = $filter("monetaryFormat")(wapData.cpm);
                $scope.showWaterfallAdProviderModal = true;
                $scope.modalShown = true;
                for(var i = 0; i < wapData.reqParams.length; i++) {
                    var param = wapData.reqParams[i];
                    $scope.restartableParams[param.displayKey] = param.value;
                }
            };

            // Retrieves WaterfallAdProvider data from the server if an instance exists.  Otherwise, create a new WaterfallAdProvider.
            $scope.editWaterfallAdProvider = function(adProviderConfig) {
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
                        $scope.flashMessage(data);
                    });
                } else {
                    // If a WaterfallAdProvider already exists, retrieve its data from the server
                    $http.get('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + adProviderConfig.waterfallAdProviderID + '/edit').success(function(wapData) {
                        setWAPData(wapData)
                    }).error(function(data) {
                        $scope.flashMessage(data);
                    });
                }
            };

            // Assembles dynamic WaterfallAdProvider fields into a standard format and checks each field for error while doing so
            var retrieveFields = function(fieldType, required) {
                return($scope.wapData[fieldType].reduce(function(fieldObj, el) {
                    var label = el.key;
                    var value = el.value;
                    if(el.dataType == "Array") {
                        var arrayValues = value.split(",").map(function(arrayValue) { return(arrayValue.trim()); });
                        fieldObj[label] = arrayValues;
                        if(required){
                            checkForErrors(label, arrayValues[0], fieldType);
                        }
                    } else {
                        fieldObj[label] = value;
                        if(required) {
                            checkForErrors(label, value, fieldType);
                        }
                    }
                    return fieldObj;
                }, {}))
            };

            // Checks if a required dynamic WaterfallAdProvider field is missing and if so, renders an error and invalidates the form.
            var checkForErrors = function(param, value, paramType) {
                if(value === undefined || value === null || value === "") {
                    $scope.errors[paramType + "-" + param] = "error";
                    $scope.invalidForm = true;
                }
            };

            // Checks WaterfallAdProvider modal form and submits update if valid.
            $scope.updateWAP = function(wapID, adProviderName) {
                $scope.errors = {};
                $scope.invalidForm = false;
                var reportingActive = $scope.wapData.reportingActive;
                var cpm = $scope.wapData.cpm;
                var wapData = {
                    configurationData: {
                        requiredParams: retrieveFields("reqParams", true),
                        reportingParams: retrieveFields("reportingParams", reportingActive ? true : false),
                        callbackParams: retrieveFields("callbackParams", false)
                    },
                    reportingActive: reportingActive,
                    appToken: $scope.appToken,
                    waterfallID: $routeParams.waterfallID,
                    cpm: cpm,
                    generationNumber: $scope.generationNumber
                };
                // Check for modified params that require an App restart
                for (var i = 0; i < $scope.wapData.reqParams.length; i++) {
                    var param = $scope.wapData.reqParams[i];
                    if (param.displayKey != "" && param.value != $scope.restartableParams[param.displayKey]) {
                        $scope.changedRestartParams[param.displayKey] = param.value;
                    }
                }
                var parsedCpm = parseFloat(cpm);
                if(isNaN(parsedCpm) || parsedCpm < 0.01) {
                    $scope.errors.cpmMessage = "eCPM must be greater than $0.00";
                    $scope.invalidForm = true;
                    $scope.errors["staticParams-cpm"] = "error";
                }
                if(!$scope.invalidForm) {
                    // Submit update for WaterfallAdProvider
                    $http.post('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + wapID, wapData).success(function(data) {
                        $scope.generationNumber = data.newGenerationNumber;
                        var adProviders = $scope.waterfallData.waterfallAdProviderList;
                        for(var i = 0; i < adProviders.length; i++) {
                            if(adProviders[i].name === adProviderName) {
                                if(adProviders[i].unconfigured) {
                                    $scope.changedRestartParams = {};
                                }
                                adProviders[i].cpm = parsedCpm;
                                adProviders[i].unconfigured = false;
                            }
                        }
                        $scope.orderOptimizedWaterfallList();
                        $scope.showWaterfallAdProviderModal = false;
                        $scope.modalShown = false;
                        var restartParams = Object.keys($scope.changedRestartParams);
                        var successMessage = adProviderName + " updated!";
                        $scope.flashMessage({message: generateWAPSuccessMesage(successMessage, restartParams), status: "success"});
                    }).error(function(data) {
                        $scope.flashMessage(data);
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

            /* Flash message logic */
            // Flashes a success/error message
            $scope.flashMessage = function(data) {
                var messages = $scope.messages.concat([{text: data.message, status: data.status}]);
                if(messages.length > 3) {
                    messages.pop();
                }
                $scope.messages = messages;
                $scope.setMessageTimeout(data.message);
            };

            // Removes a flash message from the UI
            $scope.acknowledgeMessage = function(index) {
                var messages = $scope.messages;
                messages.splice(index, 1);
                $scope.messages = messages;
            };

            // Sets message timeout and removes the last flash message
            $scope.setMessageTimeout = function(messageText) {
                $timeout(function() {
                    for(var index in $scope.messages) {
                        if($scope.messages[index].text === messageText) {
                            var messages = $scope.messages;
                            messages.splice(index, 1);
                            return $scope.messages = messages;
                        }
                    }
                }, 5000)
            };

        } ]
);
