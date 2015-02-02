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
    appCheck.validRewardAmounts = function() {
        var rewardMin = $(":input[name=rewardMin]").val();
        var rewardMax = $(":input[name=rewardMax]").val();
        if(inputChecker.validInput(rewardMin)) {
            if(parseInt(rewardMin) < 1) {
                flashMessage("Reward Minimum must be 1 or greater.", defaultErrorDiv);
                return false;
            }
            if(inputChecker.validInput(rewardMax)) {
                if(parseInt(rewardMax) < parseInt(rewardMin)) {
                    flashMessage("Reward Maximum must be greater than or equal to Reward Minimum.", defaultErrorDiv);
                    return false;
                }
            }
        }
        return true;
    };

    // Checks if Exchange Rate is 1 or greater.
    appCheck.validExchangeRate = function() {
        var exchangeRate = $(":input[name=exchangeRate]").val();
        if(inputChecker.validInput(exchangeRate)) {
            if (parseInt(exchangeRate) < 1) {
                flashMessage("Exchange Rate must be 1 or greater.", defaultErrorDiv);
                return false;
            }
        }
        return true;
    };

    // Check if all required fields are filled.
    appCheck.fieldsFilled = function(requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = $(requiredFields[i]);
            if(typeof field.val() === "string") {
                if(field.val() === "") {
                    return false;
                }
            }
        }
        return true;
    };

    // Checks for a valid callback URL when server to server callbacks are enabled.
    appCheck.validCallback = function() {
        var callbacksEnabled = $(":input[id=serverToServerEnabled]").prop("checked");
        var callbackURL = $(":input[id=callbackURL]").val();
        if(callbacksEnabled) {
            if(!(/(http|https):\/\//).test(callbackURL)) {
                flashMessage("A valid HTTP or HTTPS callback URL is required.", defaultErrorDiv);
                return false;
            }
        }
        return true;
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

            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";

            // Default div for error messages.
            var defaultErrorDiv = $("#new-app-error-message");

            // Default div for success messages.
            var defaultSuccessDiv = $("#new-app-success-message");

            $scope.checkInputs = function() {
                if(appCheck.fieldsFilled(requiredFields)) {
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
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'appsControllers', 'distributorUsersControllers']);

mediationModule.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/distributors/:distributorID/apps/new', {
        controller: 'NewAppsController',
        templateUrl: 'assets/templates/apps/newApp.html'
    }).when('/distributors/:distributorID/apps/:appID/edit', {
        controller: 'EditAppsController',
        templateUrl: 'assets/templates/apps/editApp.html'
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
        scope: {
            show: '='
        },
        replace: true, // Replace with the template below
        transclude: true, // we want to insert custom content inside the directive
        link: function(scope, element, attrs) {
            scope.dialogStyle = {};
            if (attrs.width)
                scope.dialogStyle.width = attrs.width;
            if (attrs.height)
                scope.dialogStyle.height = attrs.height;
            scope.hideModal = function() {
                scope.show = false;
            };
        },
        //template: "<div class='ng-modal' ng-show='show'><div class='ng-modal-overlay' ng-click='hideModal()'></div><div class='ng-modal-dialog' ng-style='dialogStyle'><div class='ng-modal-close' ng-click='hideModal()'>X</div><div class='ng-modal-dialog-content' ng-transclude></div></div></div>"
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

distributorUsersControllers.factory('fieldsFilled', [function(requiredFields) {
    // Check if all required fields are filled.
    return function(requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = $(requiredFields[i]);
            if(typeof field.val() === "string") {
                if(field.val() === "") {
                    return false;
                }
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
            var requiredFields = [":input[id=email]", ":input[id=password]"];
            // Default div for error messages.
            var defaultErrorDiv = $("#error-message");

            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";

            $scope.checkInputs = function() {
                if(fieldsFilled(requiredFields)) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

            // Submit form if fields are valid.
            $scope.submit = function() {
                if(!$scope.invalidForm) {
                    $http.post('/authenticate', $scope.data).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/";
                        }).
                        error(function(data, status, headers, config) {
                            flashMessage(data.message, defaultErrorDiv);
                        });
                }
            };
        }]
);

distributorUsersControllers.controller('SignupController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage', 'fieldsFilled',
    function($scope, $http, $routeParams, $window, flashMessage, fieldsFilled) {
        var requiredFields = [":input[id=company]", ":input[id=email]", ":input[id=password]", ":input[id=confirmation]"];
        // Default div for error messages.
        var defaultErrorDiv = $("#error-message");
        $scope.showTerms = false;
        $scope.toggleTerms = function() {
            $scope.showTerms = !$scope.showTerms;
        };

        $scope.invalidForm = true;
        $scope.inactiveClass = "inactive";

        $scope.checkInputs = function() {
            if(fieldsFilled(requiredFields) && $scope.data.terms) {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";
            } else {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
            }
        };

        $scope.termsTemplate = 'assets/templates/distributor_users/terms.html';

        var validPassword = function() {
            if($scope.data.password != $scope.data.confirmation) {
                flashMessage("Password does not match confirmation.", defaultErrorDiv);
                return false;
            } else {
                return true;
            }
        };

        // Submit form if fields are valid.
        $scope.submit = function() {
            if(!$scope.invalidForm && validPassword()) {
                $http.post('/distributor_users', $scope.data).
                    success(function(data, status, headers, config) {
                        $window.location.href = "/";
                    }).
                    error(function(data, status, headers, config) {
                        flashMessage(data.message, defaultErrorDiv);
                    });
            }
        };
    }]
);
