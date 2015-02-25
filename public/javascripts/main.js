// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'ngSanitize', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters', 'ui.sortable', 'ui.bootstrap']);

// Initialize controllers
var distributorUsersControllers = angular.module('distributorUsersControllers', ['ngRoute']);
var appsControllers = angular.module('appsControllers', ['ngRoute']);

// Routing
mediationModule.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/distributors/:distributorID/apps/new', {
        controller: 'NewAppsController',
        templateUrl: 'assets/templates/apps/newAppModal.html'
    }).when('/distributors/:distributorID/analytics', {
        controller: 'AnalyticsController',
        templateUrl: 'assets/templates/analytics/analytics.html'
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
                scope.showModal(false);
                scope.showWaterfallAdProviderModal = false;
                scope.showEditAppModal = false;
                scope.showNewAppModal = false;
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

mediationModule.directive('typeaheadFocus', function () {
        return {
            require: 'ngModel',
            link: function (scope, element, attr, ngModel) {
                //trigger the popup on 'click' because 'focus'
                //is also triggered after the item selection
                element.bind('click', function () {
                    var viewValue = ngModel.$viewValue;

                    //restore to null value so that the typeahead can detect a change
                    if (ngModel.$viewValue == ' ') {
                        ngModel.$setViewValue(null);
                    }

                    //force trigger the popup
                    ngModel.$setViewValue(' ');

                    //set the actual value in case there was already a value in the input
                    ngModel.$setViewValue(viewValue || ' ');
                });

                //compare function that treats the empty space as a match
                scope.emptyOrMatch = function (actual, expected) {
                    if (expected == ' ') {
                        return true;
                    }
                    return actual.toLowerCase().indexOf(expected.toLowerCase()) > -1;
                };
            }
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