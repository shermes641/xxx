// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters', 'ui.sortable']);

// Initialize controllers
var distributorUsersControllers = angular.module('distributorUsersControllers', ['ngRoute']);
var appsControllers = angular.module('appsControllers', ['ngRoute']);

// Routing
mediationModule.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/distributors/:distributorID/apps/new', {
        controller: 'NewAppsController',
        templateUrl: 'assets/templates/apps/newAppModal.html'
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
}]);

// Factories
appsControllers.factory('appCheck', [function() {
    var appCheck = {};

    var invalidNumber = function(number) {
        if(typeof number === "string") {
            return number.match(/^[0-9]+?$/) === null
        } else {
            return false;
        }
    };

    // Checks if Reward Minimum is greater than Reward Maximum.
    appCheck.validRewardAmounts = function(data) {
        var parsedRewardMin = data.rewardMin;
        if(invalidNumber(data.rewardMin) || parsedRewardMin < 1) {
            return({message: "Reward Minimum must be a valid number greater than or equal to 1.", fieldName: "rewardMin"});
        }
        if(data.rewardMax !== "") {
            if(invalidNumber(data.rewardMax) || parseInt(data.rewardMax) < parsedRewardMin) {
                return({message: "Reward Maximum must be a valid number greater than or equal to Reward Minimum.", fieldName: "rewardMax"});
            }
        }
        return {};
    };

    // Checks if Exchange Rate is 1 or greater.
    appCheck.validExchangeRate = function(exchangeRate) {
        if (invalidNumber(exchangeRate) || exchangeRate < 1) {
            return({message: "Exchange Rate must be a valid number greater than or equal to 1.", fieldName: "exchangeRate"});
        } else {
            return {};
        }
    };

    // Checks for a valid callback URL when server to server callbacks are enabled.
    appCheck.validCallback = function(data) {
        var callbacksEnabled = data.serverToServerEnabled;
        var callbackURL = data.callbackURL;
        if(callbacksEnabled) {
            if(!(/(http|https):\/\//).test(callbackURL)) {
                return({message: "A valid HTTP or HTTPS callback URL is required.", fieldName: "callbackURL"});
            }
        }
        return {};
    };

    return appCheck;
}]);

mediationModule.factory('fieldsFilled', [function(data, requiredFields) {
    // Check if all required fields are filled.
    return function(data, requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = data[requiredFields[i]];
            if(field === undefined || field === "" || field === null) {
                return false
            }
        }
        return true;
    };
}]);

// Directives
mediationModule.directive('modalDialog', function($rootScope) {
    return {
        restrict: 'E',
        scope: false,
        replace: true,
        transclude: true,
        link: function(scope, element, attrs) {
            scope.dialogStyle = {};
            if (attrs.width)
                scope.dialogStyle.width = attrs.width;
            if (attrs.height)
                scope.dialogStyle.height = attrs.height;
            scope.hideModal = function() {
                scope.errors = {};
                if(scope.showTestModeConfirmationModal) {
                    scope.waterfallData.waterfall.testMode = false;
                }
                scope.showModal(false);
                scope.showWaterfallAdProviderModal = false;
                scope.showEditAppModal = false;
                scope.showNewAppModal = false;
                scope.showTestModeConfirmationModal = false;
                $rootScope.bodyClass = "";
            };

            // Add body class to prevent scrolling when modal open
            scope.showModal = function(display) {
                $rootScope.bodyClass = display ? "modal-active" : "";
                scope.modalShown = display;
            };

        },
        templateUrl: "assets/templates/apps/modal.html"
    };
});

// Filters
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
