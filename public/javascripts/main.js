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
    }).when('/distributors/:distributorID/waterfalls/:waterfallID/edit', {
        controller: 'WaterfallController'
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

appsControllers.directive('modalDialog', function() {
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
