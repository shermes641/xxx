appsControllers.controller( 'NewAppsController', [ '$scope', '$window', '$http', '$routeParams', 'appCheck',
        function( $scope, $window, $http, $routeParams, appCheck ) {
            $('body').addClass('new-app-page');

            $scope.newAppModalTitle = "Welcome to hyprMediate!";
            $scope.newAppPage = true;
            $scope.invalidForm = true;
            $scope.inactiveClass = "inactive";

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

