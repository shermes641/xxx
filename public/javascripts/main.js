var appsControllers = angular.module('appsControllers', ['ngRoute']);

appsControllers.factory('appCheck', ['flashMessage', function(flashMessage) {
    // Default div for error messages.
    var defaultErrorDiv = $("#error-message");

    var appCheck = {};

    // Checks if Reward Minimum is greater than Reward Maximum.
    appCheck.validRewardAmounts = function(data) {
        var rewardMin = data.rewardMin;
        var rewardMax = data.rewardMax;
        if(rewardMin < 1) {
            return({message: "Reward Minimum must be 1 or greater.", fieldName: "rewardMin"});
        }
        if(rewardMax !== null) {
            if(rewardMax < rewardMin) {
                return({message: "Reward Maximum must be greater than or equal to Reward Minimum.", fieldName: "rewardMax"});
            }
        }
        return {};
    };

    // Checks if Exchange Rate is 1 or greater.
    appCheck.validExchangeRate = function(exchangeRate) {
        if (exchangeRate < 1) {
            return({message: "Exchange Rate must be 1 or greater.", fieldName: "exchangeRate"});
        } else {
            return {};
        }
    };

    // Check if all required fields are filled.
    appCheck.fieldsFilled = function(data, requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = data[requiredFields[i]];
            if(field === undefined || field === null || field === "") {
                return false
            }
        }
        return true;
    };

    // Checks for a valid callback URL when server to server callbacks are enabled.
    appCheck.validCallback = function(data) {
        var callbacksEnabled = data.serverToServerEnabled;//$(":input[id=serverToServerEnabled]").prop("checked");
        var callbackURL = data.callbackURL;//$(":input[id=callbackURL]").val();
        if(callbacksEnabled) {
            if(!(/(http|https):\/\//).test(callbackURL)) {
                return({message: "A valid HTTP or HTTPS callback URL is required.", fieldName: "callbackURL"});
            }
        }
        return {};
    };

    return appCheck;
}]);

appsControllers.factory('flashMessage', [function(message, div) {
    return function(message, div) {
        div.html(message).fadeIn();
        div.delay(3000).fadeOut("slow");
    };
}]);

appsControllers.controller( 'NewAppsController', [ '$scope', '$window', '$http', '$routeParams', 'appCheck', 'flashMessage',
        function( $scope, $window, $http, $routeParams, appCheck, flashMessage ) {
            $('body').addClass('new-app-page');

            $scope.newAppModalTitle = "Welcome to hyprMediate!";
            $scope.newAppPage = true;
            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";

            // Default div for success messages.
            var defaultSuccessDiv = $("#full-success-message");

            $scope.checkInputs = function() {
                var requiredFields = ['appName', 'currencyName', 'rewardMin', 'exchangeRate'];
                if(appCheck.fieldsFilled($scope.newApp, requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

            $scope.newApp = {appName: null, currencyName: null, rewardMin: null, rewardMax: null, roundUp: true};

            // Submit form if fields are valid.
            $scope.submitNewApp = function() {
                $scope.errors = {};
                var errorObjects = [appCheck.validRewardAmounts($scope.newApp), appCheck.validExchangeRate($scope.newApp.exchangeRate)];
                if(checkAppFormErrors($scope.newApp, errorObjects)) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
                            $scope.systemMessage = "Your confirmation email will arrive shortly.";
                            window.location.href = "/distributors/"+$routeParams.distributorID+"/waterfalls/edit";
                        }).
                        error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            } else {
                                $scope.systemMessage = data.message;
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
        }]
);

// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters', 'ui.sortable']);

mediationModule.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/distributors/:distributorID/apps/new', {
        controller: 'NewAppsController',
        templateUrl: 'assets/templates/apps/newAppModal.html'
    }).when('/distributors/:distributorID/apps/:appID/edit', {
        controller: 'EditAppsController',
        templateUrl: 'assets/templates/apps/editApp.html'
    }).when('/distributors/:distributorID/waterfalls/:waterfallID/edit', {
        controller: 'WaterfallController',
        templateUrl: 'assets/templates/waterfalls/edit.html'
    }).when('distributors/:distributorID/apps', {
        controller: 'NewAppsController'
    }).when('/signup', {
        controller: 'SignupController',
        templateUrl: 'assets/templates/distributor_users/signup.html'
    }).when('/login', {
        controller: 'LoginController',
        templateUrl: 'assets/templates/distributor_users/login.html'
    });
    $locationProvider.html5Mode(true);
    $locationProvider.hashPrefix('!');
}]);

mediationModule.directive('modalDialog', function() {
    return {
        restrict: 'E',
        scope: false,
        replace: true, // Replace with the template below
        transclude: true, // we want to insert custom content inside the directive
        link: function(scope, element, attrs) {
            scope.dialogStyle = {};
            if (attrs.width)
                scope.dialogStyle.width = attrs.width;
            if (attrs.height)
                scope.dialogStyle.height = attrs.height;
            scope.hideModal = function() {
                scope.errors = {};
                scope.modalShown = false;
                scope.showWaterfallAdProviderModal = false;
                scope.showEditAppModal = false;
                scope.showNewAppModal = false;
            };
        },
        templateUrl: "assets/templates/apps/modal.html"
    };
});

appsControllers.controller('IndexAppsController', ['$scope', '$routeParams', function($scope, $routeParams) {
    $scope.modalShown = false;
    $scope.toggleModal = function() {
        $scope.modalShown = !$scope.modalShown;
    };
}]);

var distributorUsersControllers = angular.module('distributorUsersControllers', ['ngRoute']);

distributorUsersControllers.factory('fieldsFilled', [function(data, requiredFields) {
    // Check if all required fields are filled.
    return function(data, requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = data[requiredFields[i]];
            if(field === "" || field === null) {
                return false
            }
        }
        return true;
    };
}]);

distributorUsersControllers.factory('flashMessage', [function(message, div) {
    return function(message, div) {
        div.html(message).fadeIn();
        div.delay(3000).fadeOut("slow");
    };
}]);

distributorUsersControllers.controller('LoginController', ['$scope', '$http', '$routeParams', '$window', 'fieldsFilled',
        function($scope, $http, $routeParams, $window, fieldsFilled) {
            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";
            $scope.errors = {};

            $scope.checkInputs = function() {
                var requiredFields = ["email", "password"];
                if(fieldsFilled($scope.data, requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

            // Submit form if fields are valid.
            $scope.submit = function() {
                $scope.errors = {};
                if(!$scope.invalidForm) {
                    $http.post('/authenticate', $scope.data).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/";
                        }).
                        error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            } else {
                                flashMessage(data.message, defaultErrorDiv);
                            }
                        });
                }
            };
        }]
);

distributorUsersControllers.controller('SignupController', ['$scope', '$http', '$routeParams', '$window', 'fieldsFilled',
    function($scope, $http, $routeParams, $window, fieldsFilled) {
        $scope.showTerms = false;
        $scope.invalidForm = true;
        $scope.inactiveClass = "inactive";
        $scope.errors = {};
        $scope.termsTemplate = 'assets/templates/distributor_users/terms.html';

        $scope.toggleTerms = function() {
            $scope.showTerms = !$scope.showTerms;
        };

        $scope.checkInputs = function() {
            var requiredFields = ['company', 'email', 'password', 'confirmation'];
            if(fieldsFilled($scope.data, requiredFields) && $scope.data.terms) {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";
            } else {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
            }
        };

        var checkPassword = function() {
            if($scope.data.password.length < 8) {
                return({message: "Password must be a minimum of 8 characters.", fieldName: "password"});
            }
            if($scope.data.password != $scope.data.confirmation) {
                return({message: "Password does not match confirmation.", fieldName: "password"});
            }
            return {};
        };

        var validFields = function(errorObjects) {
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

        // Submit form if fields are valid.
        $scope.submit = function() {
            $scope.errors = {};
            if(!$scope.invalidForm && validFields([checkPassword()])) {
                $http.post('/distributor_users', $scope.data).
                    success(function(data, status, headers, config) {
                        $window.location.href = "/distributors/"+data.distributorID+"/apps/new";
                    }).
                    error(function(data, status, headers, config) {
                        if(data.fieldName) {
                            $scope.errors[data.fieldName] = data.message;
                            $scope.errors[data.fieldName + "Class"] = "error";
                        } else {
                            flashMessage(data.message, defaultErrorDiv);
                        }
                    });
            }
        };
    }]
);

angular.module('eCPMFilter', []).filter('monetaryFormat', function() {
    return function(value) {
        if(value === null) {
            return "";
        } else {
            var formatted = Math.floor(100 * value) / 100;
            return parseFloat(formatted).toFixed(2);
        }
    };
});

angular.module('waterfallFilters', []).filter('waterfallStatus', function() {
    return function(status) {
        return status ? "Deactivate" : "Activate";
    };
});

mediationModule.controller( 'WaterfallController', [ '$scope', '$http', '$routeParams', 'appCheck', '$filter', '$timeout',
        function( $scope, $http, $routeParams, appCheck, $filter, $timeout ) {
            // Angular Templates
            $scope.appList = 'assets/templates/waterfalls/appList.html';
            $scope.subHeader = 'assets/templates/sub_header.html';
            $scope.editAppModal = 'assets/templates/apps/editAppModal.html';
            $scope.newAppModal = 'assets/templates/apps/newAppModal.html';
            $scope.editWaterfallAdProviderModal = 'assets/templates/waterfall_ad_providers/edit.html';

            $scope.page = 'waterfall';
            $scope.newAppModalTitle = "Create New App";
            $scope.showWaterfallAdProviderModal = false;
            $scope.adProviderModalShown = false;
            $scope.showCodeBlock = false;
            $scope.disableTestModeToggle = false;
            $scope.systemMessage = "";
            $scope.messages = [];

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

            $scope.toggleCodeBlock = function() {
                $scope.showCodeBlock = !$scope.showCodeBlock;
            };

            $scope.sortableOptions = {
                stop: function(e, ui) {
                    $scope.setWaterfallOrder();
                    $scope.updateWaterfall();
                }
            };

            var content = $(".split_content");

            // Stores params that have been changed which require an app restart.
            $scope.appRestartParams = {};

            // Rearranges the waterfall order list either by eCPM or original order.
            $scope.orderList = function() {
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

            // Retrieves ordered list of ad providers who are either active or inactive
            $scope.providersByActive = function(active) {
                return $scope.waterfallData.waterfallAdProviderList.filter(function(li) { return(li.active === active) });
            };

            // Default div for error messages.
            var defaultErrorDiv = $("#error-message");

            // Default div for success messages.
            var defaultSuccessDiv = $("#success-message");

            $scope.modalShown = false;
            $scope.toggleEditAppModal = function() {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";
                $http.get('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID + '/edit').success(function(data) {
                    $scope.data = data;
                }).error(function(data) {
                });
                $scope.showEditAppModal = !$scope.showEditAppModal;
                $scope.modalShown = !$scope.modalShown;
            };

            $scope.toggleNewAppModal = function() {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
                $scope.newApp = {appName: null, currencyName: null, rewardMin: null, rewardMax: null, roundUp: true};
                $scope.showNewAppModal = !$scope.showNewAppModal;
                $scope.modalShown = !$scope.modalShown;
            };

            $scope.closeWapModal = function() {
                $scope.modalShown = false;
                $scope.showWaterfallAdProviderModal = false;
            };

            $scope.checkInputs = function(data) {
                var requiredFields = ['appName', 'currencyName', 'rewardMin', 'exchangeRate'];
                if(appCheck.fieldsFilled(data, requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
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

            // Submit form if fields are valid.
            $scope.submitNewApp = function() {
                $scope.errors = {};
                var errorObjects = [appCheck.validRewardAmounts($scope.newApp), appCheck.validExchangeRate($scope.newApp.exchangeRate)];
                if(checkAppFormErrors($scope.newApp, errorObjects)) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
                            $scope.toggleNewAppModal();
                            $scope.flashMessage(data);
                        }).
                        error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };


            var getWapData = function(wapID) {
                $http.get('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + wapID + '/edit').success(function(wapData) {
                    $scope.wapData = wapData;
                    $scope.wapData.cpm = $filter("monetaryFormat")(wapData.cpm);
                    $scope.showWaterfallAdProviderModal = true;
                    $scope.modalShown = true
                }).error(function(data) {
                    $scope.flashMessage(data);
                });
            };

            $scope.editWaterfallAdProvider = function(adProviderConfig) {
                $scope.errors = {};
                $scope.invalidForm = false;
                if(adProviderConfig.newRecord) {
                    var params = {};
                    var path = "/distributors/" + $scope.distributorID + "/waterfall_ad_providers";
                    var generationNumber = $scope.generationNumber;
                    params["waterfallID"] = $routeParams.waterfallID;
                    params["appToken"] = $scope.appToken;
                    params["generationNumber"] = generationNumber;
                    params["configurable"] = adProviderConfig.configurable;
                    params["adProviderID"] = adProviderConfig.waterfallAdProviderID;
                    params["cpm"] = adProviderConfig.cpm;
                    $http.post(path, params).success(function(data) {
                        for(var i = 0; i < $scope.waterfallData.waterfallAdProviderList.length; i++) {
                            var provider = $scope.waterfallData.waterfallAdProviderList[i];
                            if(provider.waterfallAdProviderID === params["adProviderID"]) {
                                provider.newRecord = false;
                                provider.waterfallAdProviderID = data.wapID;
                            }
                        }
                        $scope.generationNumber = data.newGenerationNumber;
                        getWapData(data.wapID);
                    }).error(function(data) {
                        $scope.flashMessage(data);
                    });
                } else {
                    getWapData(adProviderConfig.waterfallAdProviderID);
                }
            };

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

            var checkForErrors = function(param, value, paramType) {
                if(value === undefined || value === null || value === "") {
                    $scope.errors[paramType + "-" + param] = "error";
                    $scope.invalidForm = true;
                }
            };

            $scope.updateWap = function(wapID, adProviderName) {
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
                checkForErrors("cpm", cpm, "staticParams");
                var parsedCpm = parseFloat(cpm);
                if(isNaN(cpm) || parsedCpm < 0.01) {
                    $scope.errors.cpmMessage = "eCPM must be greater than $0.00";
                    $scope.invalidForm = true;
                    $scope.errors["staticParams-cpm"] = "error";
                }
                if(!$scope.invalidForm) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + wapID, wapData).success(function(data) {
                        $scope.generationNumber = data.newGenerationNumber;
                        var adProviders = $scope.waterfallData.waterfallAdProviderList;
                        for(var i = 0; i < adProviders.length; i++) {
                            if(adProviders[i].name === adProviderName) {
                                adProviders[i].cpm = parsedCpm;
                                adProviders[i].unconfigured = false;
                            }
                        }
                        $scope.orderList();
                        $scope.showWaterfallAdProviderModal = false;
                        $scope.modalShown = false;
                        $scope.flashMessage({message: adProviderName + " updated!", status: "success"});
                    }).error(function(data) {
                        $scope.flashMessage(data);
                    });
                }
            };

            $scope.flashMessage = function(data) {
                var messages = $scope.messages.concat([{text: data.message, status: data.status}]);
                if(messages.length > 3) {
                    messages.pop();
                }
                $scope.messages = messages;
                $scope.setMessageTimeout(data.message);
            };

            $scope.acknowledgeMessage = function(index) {
                var messages = $scope.messages;
                messages.splice(index, 1);
                $scope.messages = messages;
            };

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
                });
            };

            var checkTestModeToggle = function() {
                var activeAdProviders = $scope.waterfallData.waterfallAdProviderList.filter(function(el, index) { return(el.active); });
                return (activeAdProviders.length < 1 && $scope.waterfallData.waterfall.testMode);
            };

            $scope.toggleTestMode = function() {
                if(!$scope.disableTestModeToggle) {
                    $scope.updateWaterfall();
                    $scope.disableTestModeToggle = checkTestModeToggle();
                } else {
                    $scope.waterfallData.waterfall.testMode = !$scope.waterfallData.waterfall.testMode;
                    $scope.flashMessage({message: "You must activate at least one Ad Provider", status: "error"})
                }
            };

            $scope.toggleOptimizedMode = function() {
                $scope.sortableOptions.disabled = !$scope.sortableOptions.disabled;
                if($scope.waterfallData.waterfall.optimizedOrder) {
                    $scope.orderList();
                }
                $scope.updateWaterfall();
            };

            $scope.toggleWapStatus = function(adProviderConfig) {
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
                $scope.orderList();
            };

            // Submit form if fields are valid.
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
                        }).
                        error(function(data, status, headers, config) {
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };

            $scope.setWaterfallOrder = function() {
                for (var index in $scope.waterfallData.waterfallAdProviderList) {
                    $scope.waterfallData.waterfallAdProviderList[index].waterfallOrder = parseInt(index);
                }
            };
        } ]
);
