var appsControllers = angular.module('appsControllers', ['ngRoute']);

appsControllers.factory('inputChecker', [function(input) {
    var inputChecker = {};

    // Checks if input is not empty before performing any other validations.
    inputChecker.validInput = function(input) {
        return (typeof input === "string" && input !== "")
    };

    return inputChecker;
}]);

appsControllers.factory('appCheck', ['inputChecker', 'flashMessage', function(inputChecker, flashMessage) {
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
            if(field === "" || field === null) {
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

appsControllers.controller( 'EditAppsController', ['$scope', '$http', '$routeParams', 'appCheck', 'flashMessage',
    function( $scope, $http, $routeParams, appCheck, flashMessage ) {
        // Default div for error messages.
        var defaultErrorDiv = $("#error-message");

        // Default div for success messages.
        var defaultSuccessDiv = $("#success-message");

        // Submit form if fields are valid.
        $scope.submit = function() {
            if(appCheck.validRewardAmounts() && appCheck.validExchangeRate() && appCheck.validCallback()) {
                $http.post('/distributors/' + $routeParams.distributorID + '/apps/' + $routeParams.appID, $scope.data).
                    success(function(data, status, headers, config) {
                        $scope.data.generationNumber = data.generationNumber;
                        flashMessage(data.message, defaultSuccessDiv);
                    }).
                    error(function(data, status, headers, config) {
                        flashMessage(data.message, defaultErrorDiv);
                    });
            }
        };

        $http.get('/distributors/' + $routeParams.distributorID + '/apps/' + $routeParams.appID + '/edit').success(function(data) {
            $scope.data = data;
        }).error(function(data) {
        });
    }]
);

appsControllers.controller( 'NewAppsController', [ '$scope', '$http', '$routeParams', 'appCheck', 'flashMessage',
        function( $scope, $http, $routeParams, appCheck, flashMessage ) {
            var requiredFields = [":input[id=appName]", ":input[id=currencyName]", ":input[id=exchangeRate]", ":input[id=rewardMin]"];
            $('body').addClass('new-app-page');
            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";

            // Default div for error messages.
            var defaultErrorDiv = $("#new-app-error-message");

            // Default div for success messages.
            var defaultSuccessDiv = $("#new-app-success-message");

            $scope.checkInputs = function() {
                if(appCheck.fieldsFilled($scope.data, requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

            // Submit form if fields are valid.
            $scope.submit = function() {
                if(appCheck.validRewardAmounts() && appCheck.validExchangeRate()) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.data).
                        success(function(data, status, headers, config) {
                            flashMessage(data.message, defaultSuccessDiv);
                        }).
                        error(function(data, status, headers, config) {
                            flashMessage(data.message, defaultErrorDiv);
                        });
                }
            };
        }]
);

// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters']);

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

distributorUsersControllers.controller('LoginController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage', 'fieldsFilled',
        function($scope, $http, $routeParams, $window, flashMessage, fieldsFilled) {
            // Default div for error messages.
            var defaultErrorDiv = $("#error-message");

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

distributorUsersControllers.controller('SignupController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage', 'fieldsFilled',
    function($scope, $http, $routeParams, $window, flashMessage, fieldsFilled) {
        // Default div for error messages.
        var defaultErrorDiv = $("#error-message");
        $scope.showTerms = false;
        $scope.toggleTerms = function() {
            $scope.showTerms = !$scope.showTerms;
        };

        $scope.invalidForm = true;
        $scope.inactiveClass = "inactive";
        $scope.errors = {};

        $scope.checkInputs = function() {
            var requiredFields = ['companyName', 'email', 'password', 'confirmation'];
            if(fieldsFilled($scope.data, requiredFields) && $scope.data.terms) {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";
            } else {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
            }
        };

        $scope.termsTemplate = 'assets/templates/distributor_users/terms.html';

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

angular.module('eCPMFilter', []).filter('monetaryFormat', function() {
    return function(value) {
        var formatted = Math.floor(100 * value) / 100;
        return parseFloat(formatted).toFixed(2);
    };
});

angular.module('waterfallFilters', []).filter('waterfallStatus', function() {
    return function(status) {
        return status ? "Deactivate" : "Activate";
    };
});

mediationModule.controller( 'WaterfallController', [ '$scope', '$http', '$routeParams', 'appCheck', 'flashMessage',
        function( $scope, $http, $routeParams, appCheck, flashMessage ) {
            $http.get('/distributors/' + $routeParams.distributorID + '/waterfalls/' + $routeParams.waterfallID + '/waterfall_info').success(function(data) {
                $scope.waterfallData = data;
                $scope.appID = data.waterfall.appID;
                $scope.distributorID = $routeParams.distributorID;
                $scope.generationNumber = data.generationNumber;
                $scope.appToken = data.waterfall.appToken;
            }).error(function(data) {
            });

            $scope.appList = 'assets/templates/waterfalls/appList.html';

            var content = $(".split_content");
            // Distributor ID to be used in AJAX calls.
            $scope.distributorID = content.attr("data-distributor-id");

            // Waterfall ID to be used in AJAX calls.
            $scope.waterfallID = content.attr("data-waterfall-id");

            // App Token to be used in AJAX calls.
            $scope.appToken = $(".app-token").attr("data-app-token");

            // Selector for button which toggles waterfall optimization.
            $scope.optimizeToggleButton = $(":checkbox[name=optimized-order]");

            // Selector for button which toggles waterfall from live to test mode.
            $scope.testModeButton = $(":checkbox[name=test-mode]");

            // Drop down menu to select the desired waterfall edit page.
            $scope.waterfallSelection = $(":input[id=waterfall-selection]");

            // Stores params that have been changed which require an app restart.
            $scope.appRestartParams = {};

            // Rearranges the waterfall order list either by eCPM or original order.
            $scope.orderList = function(orderAttr, ascending) {
                var newOrder = $scope.providersByActive("true").sort(function(li1, li2) {
                    return (ascending ? Number($(li1).attr(orderAttr)) - Number($(li2).attr(orderAttr)) : Number($(li2).attr(orderAttr)) - Number($(li1).attr(orderAttr)))
                });
                var inactive = $scope.providersByActive("false");
                newOrder.push.apply(newOrder, inactive);
                $scope.appendNewList(newOrder);
            };

            // Displays the updated list order in view.
            $scope.appendNewList = function(newOrder) {
                var list = $("#waterfall-list");
                $.each(newOrder, function(index, listItem){
                    list.append(listItem);
                })
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
                            flashMessage(data.message, defaultSuccessDiv);
                            $scope.toggleNewAppModal();
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


            $scope.editAppModal = 'assets/templates/apps/editAppModal.html';

            $scope.newAppModal = 'assets/templates/apps/newAppModal.html';

            $scope.editWaterfallAdProviderModal = 'assets/templates/waterfall_ad_providers/edit.html';

            $scope.showWaterfallAdProviderModal = false;

            $scope.adProviderModalShown = false;

            $scope.editWaterfallAdProvider = function(adProviderConfig) {
                $scope.invalidForm = false;
                if(adProviderConfig.newRecord) {
                    var params = {};
                    var path = "/distributors/" + $scope.distributorID + "/waterfall_ad_providers";
                    var generationNumber = $scope.generationNumber;
                    params["waterfallID"] = $routeParams.waterfallID;
                    params["appToken"] = $scope.appToken;
                    params["waterfallOrder"] = null;
                    params["generationNumber"] = generationNumber;
                    params["configurable"] = adProviderConfig.configurable;
                    params["cpm"] = null;
                    params["adProviderID"] = adProviderConfig.waterfallAdProviderID;
                    $http.post(path, params).success(function(data) {
                    }).error(function(data){
                    });
                    /*
                    $.ajax({
                        url: path,
                        type: 'POST',
                        contentType: "application/json",
                        data: JSON.stringify(params),
                        success: function(result) {
                            var item = $("li[id=true-" + params["adProviderID"] + "]");
                            var configureButton = item.find("button[name=configure-wap]");
                            item.attr("data-new-record", "false");
                            item.attr("id", "false-" + result.wapID);
                            item.attr("data-id", result.wapID);
                            configureButton.show();
                            if(newRecord) {
                                retrieveConfigData(result.wapID, newRecord);
                            }
                            $(".split_content").attr("data-generation-number", result.newGenerationNumber)
                            $scope.flashMessage(result.message, $("#waterfall-edit-success"))
                        },
                        error: function(result) {
                            $scope.flashMessage(result.message, $("#waterfall-edit-error"));
                        }
                        */


                } else {
                    $http.get('/distributors/' + $routeParams.distributorID + '/waterfall_ad_providers/' + adProviderConfig.waterfallAdProviderID + '/edit').success(function(data) {
                        $scope.wapData = data;
                    }).error(function(data) {
                    });
                }
                $scope.showWaterfallAdProviderModal = true;
                $scope.modalShown = true
            };

            // Submit form if fields are valid.
            $scope.submitEditApp = function() {
                $scope.errors = {};
                var errorObjects = [appCheck.validRewardAmounts($scope.data), appCheck.validExchangeRate($scope.data.exchangeRate), appCheck.validCallback($scope.data)];
                if(checkAppFormErrors($scope.data, errorObjects)) {
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps/' + $scope.appID, $scope.data).
                        success(function(data, status, headers, config) {
                            $scope.data.generationNumber = data.generationNumber;
                            flashMessage(data.message, defaultSuccessDiv);
                            $scope.toggleEditAppModal();
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

            // Retrieves ordered list of ad providers who are either active or inactive
            $scope.providersByActive = function(active) {
                return $("#waterfall-list").children("li").filter(function(index, li) { return($(li).attr("data-active") === active) });
            };

            // Updates waterfall properties via AJAX.
            $scope.postUpdate = function() {
                var path = "/distributors/" + $scope.distributorID + "/waterfalls/" + $scope.waterfallID;
                $.ajax({
                    url: path,
                    type: 'POST',
                    contentType: "application/json",
                    data: $scope.updatedData(),
                    success: function(result) {
                        $(".split_content").attr("data-generation-number", result.newGenerationNumber);
                        $scope.flashMessage(result.message, $("#waterfall-edit-success"));
                        if(optimizeToggleButton.is(":checked")) {
                            $scope.orderList("data-cpm", false);
                        }
                    },
                    error: function(result) {
                        $scope.flashMessage(result.responseJSON.message, $("#waterfall-edit-error"));
                    }
                });
            };

            // Retrieves configuration data for a waterfall ad provider.
            $scope.retrieveConfigData = function(waterfallAdProviderID, newRecord) {
                var path = "/distributors/" + $scope.distributorID + "/waterfall_ad_providers/" + waterfallAdProviderID + "/edit";
                $.ajax({
                    url: path,
                    data: {app_token: $scope.appToken},
                    type: 'GET',
                    success: function(data) {
                        $("#edit-waterfall-ad-provider").html(data).dialog({
                            modal: true,
                            open: function() {
                                $(".split_content.waterfall_list").addClass("unclickable");
                                $("#modal-overlay").toggle();
                            },
                            close: function() {
                                $(".split_content.waterfall_list").removeClass("unclickable");
                                $("#modal-overlay").toggle();
                            }
                        }).dialog("open");
                        if(!newRecord) {
                            $scope.setRefreshOnRestartListeners();
                        }
                        $(".ui-dialog-titlebar").hide();
                    },
                    error: function(data) {
                        $scope.flashMessage(data.responseJSON.message, $("#waterfall-edit-error"));
                    }
                });
            };

            // Creates waterfall ad provider via AJAX.
            $scope.createWaterfallAdProvider = function(params, newRecord) {
                var path = "/distributors/" + $scope.distributorID + "/waterfall_ad_providers";
                var generationNumber = $(".split_content").attr("data-generation-number");
                params["waterfallID"] = $scope.waterfallID;
                params["appToken"] = $scope.appToken;
                params["waterfallOrder"] = "";
                params["generationNumber"] = generationNumber;
                $.ajax({
                    url: path,
                    type: 'POST',
                    contentType: "application/json",
                    data: JSON.stringify(params),
                    success: function(result) {
                        var item = $("li[id=true-" + params["adProviderID"] + "]");
                        var configureButton = item.find("button[name=configure-wap]");
                        item.attr("data-new-record", "false");
                        item.attr("id", "false-" + result.wapID);
                        item.attr("data-id", result.wapID);
                        configureButton.show();
                        if(newRecord) {
                            retrieveConfigData(result.wapID, newRecord);
                        }
                        $(".split_content").attr("data-generation-number", result.newGenerationNumber)
                        $scope.flashMessage(result.message, $("#waterfall-edit-success"))
                    },
                    error: function(result) {
                        $scope.flashMessage(result.message, $("#waterfall-edit-error"));
                    }
                });
            };

            // Retrieves current list order and value of waterfall name field.
            $scope.updatedData = function() {
                var adProviderList = $scope.providersByActive("true");
                var optimizedOrder = optimizeToggleButton.prop("checked").toString();
                var testMode = testModeButton.prop("checked").toString();
                var generationNumber = $(".split_content").attr("data-generation-number");
                adProviderList.push.apply(adProviderList, $scope.providersByActive("false").length > 0 ? $scope.providersByActive("false") : []);
                var order = adProviderList.map(function(index, el) {
                    return({
                        id: $(this).attr("data-id"),
                        newRecord: $(this).attr("data-new-record"),
                        active: $(this).attr("data-active"),
                        waterfallOrder: index.toString(),
                        cpm: $(this).attr("data-cpm"),
                        configurable: $(this).attr("data-configurable"),
                        pending: $(this).attr("data-pending")
                    });
                }).get();
                return(JSON.stringify({adProviderOrder: order, optimizedOrder: optimizedOrder, testMode: testMode, appToken: appToken, generationNumber: generationNumber}));
            };

            // Displays success or error of AJAX request.
            $scope.flashMessage = function(message, div) {
                div.html(message).fadeIn();
                div.delay(3000).fadeOut("slow");
            };

            if( $scope.optimizeToggleButton.prop("checked") ) {
                // Order ad providers by eCPM, disable sortable list, and hide draggable icon if waterfall has optimized_order set to true.
                $scope.orderList( "data-cpm", false );
                //$( "#waterfall-list" ).sortable( "disable" );
                $( ".waterfall-drag" ).addClass('disabled');
            } else {
                // Enable sortable list if waterfall has optimized_order set to true.
                //$( "#waterfall-list" ).sortable( "enable" );
            }

            // Adds event listeners to appropriate inputs when the WaterfallAdProvider edit modal pops up.
            $scope.setRefreshOnRestartListeners = function() {
                $( ":input[data-refresh-on-app-restart=true]" ).change( function( event ) {
                    var param = $( event.target.parentElement ).children( "label" ).html();
                    appRestartParams[ param ] = true;
                } );
            };

            // Initiates AJAX request to update waterfall.
            $( ":button[name=submit]" ).click( function() {
                $scope.postUpdate();
            } );

            // Direct the user to the selected Waterfall edit page.
            $scope.waterfallSelection.change(function() {
                window.location.href = waterfallSelection.val();
            });

            // Controls activation/deactivation of each ad provider in a waterfall.
            $("button[name=status]").click(function(event) {
                var listItem = $(event.target).parents("li");
                var originalVal = listItem.attr("data-active") === "true";
                listItem.children(".hideable").toggleClass("hidden-wap-info", originalVal);
                listItem.attr("data-active", (!originalVal).toString());
                listItem.toggleClass("inactive");
                if(listItem.attr("data-new-record") === "true") {
                    $scope.createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: true});
                } else {
                    $scope.postUpdate();
                }
            });

            // Opens modal for editing waterfall ad provider configuration info.
            $(":button[name=configure-wap]").click(function(event) {
                var listItem = $(event.target).parents("li");
                var waterfallAdProviderID = listItem.attr("data-id");
                if(listItem.attr("data-new-record") === "true") {
                    $scope.createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: false}, true);
                } else {
                    $scope.retrieveConfigData(waterfallAdProviderID);
                }
            });

            /*
            // Click event for when Optimized Mode is toggled.
            $scope.optimizeToggleButton.click(function() {
                $(".waterfall-drag").toggleClass("disabled");
                var sortableOption;
                if(optimizeToggleButton.prop("checked")) {
                    sortableOption = "disable";
                    $scope.orderList("data-cpm", false);
                } else {
                    sortableOption = "enable";
                }
                $("#waterfall-list").sortable(sortableOption);
                $scope.postUpdate();
            });
            */


            /*
            // Click event for when Test Mode is toggled.
            $scope.testModeButton.click(function(event) {
                var waterfallListItems = $("#waterfall-list").children("li");
                var newRecords = waterfallListItems.filter(function(index, li) { return($(li).attr("data-new-record") === "true") }).length;
                var allRecords = waterfallListItems.length;
                // Prevent the user from setting the waterfall to live without configuring any ad providers.
                if(newRecords == allRecords) {
                    event.preventDefault();
                    $scope.flashMessage("You must activate an ad provider before the waterfall can go live.", $("#waterfall-edit-error"));
                } else {
                    $scope.postUpdate();
                }
            });
            */

            // Sends AJAX request when waterfall order is changed via drag and drop.
            $("#waterfall-list").on("sortdeactivate", function(event, ui) {
                $scope.postUpdate();
            });

            $scope.showCodeBlock = false;
            $scope.toggleCodeBlock = function() {
                $scope.showCodeBlock = !$scope.showCodeBlock;
            };
        } ]
);
