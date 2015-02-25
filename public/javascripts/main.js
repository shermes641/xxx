// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'ngSanitize', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters', 'requiredFieldFilters', 'ui.sortable', 'ui.bootstrap', 'escapeHtmlFilters']);

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
                scope.dialogStyle.overflowY = scope.showEditAppModal ? "visible" : "auto";
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
                    if(ngModel.$viewValue == ' ') {
                        ngModel.$setViewValue(null);
                    }

                    //force trigger the popup
                    ngModel.$setViewValue(' ');

                    //set the actual value in case there was already a value in the input
                    ngModel.$setViewValue(viewValue || ' ');
                });

                //compare function that treats the empty space as a match
                scope.emptyOrMatch = function (actual, expected) {
                    if(expected == ' ') {
                        return true;
                    }
                    return actual.toString().toLowerCase().indexOf(expected.toString().toLowerCase()) > -1;
                };
            }
        };
    });

var invalidNumber = function(number) {
    if(typeof number === "string") {
        return number.match(/^[0-9]+?$/) === null
    } else {
        return false;
    }
};

mediationModule.directive('requiredInteger', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.requiredInteger = function(modelValue) {
                return !(ctrl.$isEmpty(modelValue) || invalidNumber(modelValue) || parseInt(modelValue) < 1);
            };
        }
    };
});

var greaterThanDirectiveName = 'greaterThanOrEqualTo';
mediationModule.directive(greaterThanDirectiveName, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                var comparisonModel = attrs.greaterThanOrEqualTo;
                if(ctrl.$isEmpty(viewValue)) {
                    ctrl.$setValidity(greaterThanDirectiveName, true);
                } else {
                    if(parseInt(viewValue) >= parseInt(comparisonModel)) {
                        ctrl.$setValidity(greaterThanDirectiveName, true);
                    } else {
                        ctrl.$setValidity(greaterThanDirectiveName, false);
                    }
                }
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(greaterThanDirectiveName, function(comparisonModel){
                return validate(ctrl.$viewValue);
            });
        }
    };
});

mediationModule.directive('rewardMaxValidator', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.rewardMaxValidator = function(modelValue, viewValue) {
                return !(!ctrl.$isEmpty(modelValue) && invalidNumber(modelValue));
            };
        }
    };
});

var callbackValidator = 'callbackValidator';

mediationModule.directive(callbackValidator, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                var serverToServerEnabled = attrs.callbackValidator === "true";
                if(serverToServerEnabled || !ctrl.$isEmpty(ctrl.$viewValue)) {
                    ctrl.$setValidity(callbackValidator, (/(http|https):\/\//).test(ctrl.$viewValue));
                } else {
                    ctrl.$setValidity(callbackValidator, true);
                }
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(callbackValidator, function(){
                return validate();
            });
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

angular.module('requiredFieldFilters', []).filter('conditionalRequiredField', function() {
    return function(fieldName, condition) {
        return condition ? "*" + fieldName : fieldName;
    };
});

angular.module('escapeHtmlFilters', []).filter('escapeHtml', function($sce) {
    return function(value) {
        return $sce.trustAsHtml(value);
    }
});
