mediationModule.controller( 'WaterfallController', [ '$scope', '$http', '$routeParams', '$filter', '$timeout',
        function( $scope, $http, $routeParams, $filter, $timeout ) {
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
            $scope.systemMessage = "";
            $scope.messages = [];
            $scope.errors = {};
            $scope.form = {};

            // Retrieve Waterfall data
            $scope.getWaterfallData = function() {
                $http.get('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID + '/waterfall_info').success(function(data) {
                    $scope.waterfallData = data;
                    $scope.appName = data.waterfall.appName;
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

            // Checks status of WaterfallAdProviders to determine if the test mode toggle should be disabled
            var checkTestModeToggle = function() {
                var activeAdProviders = $scope.waterfallData.waterfallAdProviderList.filter(function(el, index) { return(el.active); });
                return (activeAdProviders.length < 1 && $scope.waterfallData.waterfall.testMode);
            };

            // Toggles test mode on/off
            $scope.toggleTestMode = function(testMode) {
                if(!$scope.disableTestModeToggle) {
                    if(testMode) {
                        $scope.waterfallData.waterfall.testMode = false;
                        $scope.showTestModeConfirmationModal = true;
                        $scope.showModal(!$scope.modalShown);
                    } else {
                        $scope.updateWaterfall();
                    }
                    $scope.disableTestModeToggle = checkTestModeToggle();
                } else {
                    $scope.waterfallData.waterfall.testMode = !$scope.waterfallData.waterfall.testMode;
                    $scope.flashMessage({message: "You must activate at least one Ad Provider", status: "error"})
                }
            };

            $scope.confirmTestMode = function() {
                $scope.waterfallData.waterfall.testMode = true;
                $scope.updateWaterfall();
                closeTestModeModal();
            };

            $scope.cancelTestMode = function() {
                $scope.waterfallData.waterfall.testMode = false;
                closeTestModeModal();
            };

            var closeTestModeModal = function() {
                $scope.showModal(false);
                $scope.showTestModeConfirmationModal = false;
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
                // Retrieve App data
                $http.get('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID + '/edit').success(function(data) {
                    $scope.data = data;
                    $scope.form.editAppForm.$setPristine();
                    $scope.form.editAppForm.$setUntouched();
                }).error(function(data) {
                    $scope.flashMessage(data);
                });

                $scope.showEditAppModal = !$scope.showEditAppModal;
                $scope.showModal(!$scope.modalShown);
            };

            $scope.closeWAPModal = function() {
                $scope.showModal(false);
                $scope.showWaterfallAdProviderModal = false;
            };

            // Open the App creation page
            $scope.toggleNewAppModal = function() {
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
                    setNumberValues("newApp");
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
                            $scope.toggleNewAppModal();
                            $scope.flashMessage(data);
                            $scope.getWaterfallData();
                        }).error(function(data, status, headers, config) {
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
                    setNumberValues("data");
                    $scope.data.generationNumber = $scope.generationNumber;
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID, $scope.data).
                        success(function(data, status, headers, config) {
                            if($scope.appName !== $scope.data.appName) {
                                var apps = $scope.waterfallData.appsWithWaterfalls;
                                $scope.appName = $scope.data.appName;
                                for(index in apps) {
                                    if(apps[index].id === $scope.appID) {
                                        apps[index].name = $scope.data.appName;
                                    }
                                }
                            }
                            $scope.generationNumber = data.generationNumber;
                            $scope.showEditAppModal = false;
                            $scope.showModal(false);
                            $scope.flashMessage(data);
                        }).error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            } else {
                                $scope.flashMessage(data);
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
                $scope.showModal(true);
                for(var i = 0; i < wapData.requiredParams.length; i++) {
                    var param = wapData.requiredParams[i];
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
                    $http.get('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + adProviderConfig.waterfallAdProviderID + '/edit', {params: {app_token: $scope.appToken}}).success(function(wapData) {
                        setWAPData(wapData)
                    }).error(function(data) {
                        $scope.flashMessage(data);
                    });
                }
            };

            // Checks dynamic WaterfallAdProvider fields for errors and sets the inline errors accordingly if found.
            var checkFieldsForErrors = function(fieldType) {
                return($scope.wapData[fieldType].map(function(el) {
                    var value = el.dataType == "Array" ? el.value.split(",").map(function(arrayValue) { return(arrayValue.trim()); })[0] : el.value;
                    if(value === undefined || value === null || value === "" || value.length < el.minLength) {
                        $scope.errors[fieldType + "-" + el.key] = "error";
                        $scope.errors[fieldType + "-" + el.key + "-message"] = el.minLength > 1 ? "This field requires at least " + el.minLength + " characters" : "Field is required";
                        $scope.invalidForm = true;
                    }
                }))
            };

            // Checks WaterfallAdProvider modal form and submits update if valid.
            $scope.updateWAP = function(wapID, adProviderName) {
                $scope.errors = {};
                $scope.invalidForm = false;
                checkFieldsForErrors("requiredParams", true);
                if($scope.wapData.reportingActive) { checkFieldsForErrors("reportingParams") }
                // Check for modified params that require an App restart
                for (var i = 0; i < $scope.wapData.requiredParams.length; i++) {
                    var param = $scope.wapData.requiredParams[i];
                    if (param.displayKey != "" && param.value != $scope.restartableParams[param.displayKey]) {
                        $scope.changedRestartParams[param.displayKey] = param.value;
                    }
                }
                var parsedCpm = parseFloat($scope.wapData.cpm);
                if(isNaN(parsedCpm) || parsedCpm < 0 || ($scope.wapData.cpm.match(/^[0-9]{0,}([\.][0-9]+)?$/) === null)) {
                    $scope.errors.cpmMessage = "eCPM must be a valid number greater than or equal to $0.00";
                    $scope.invalidForm = true;
                    $scope.errors["staticParams-cpm"] = "error";
                }
                if(!$scope.invalidForm) {
                    $scope.wapData.generationNumber = $scope.generationNumber;
                    $scope.wapData.appToken = $scope.appToken;
                    $scope.wapData.waterfallID = $routeParams.waterfallID;
                    // Submit update for WaterfallAdProvider
                    $http.post('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + wapID, $scope.wapData).success(function(data) {
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
                        $scope.showModal(false);
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
