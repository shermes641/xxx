appsControllers.controller( 'NewAppsController', [ '$scope', '$window', '$http', '$routeParams',
        function( $scope, $window, $http, $routeParams ) {
            $('body').addClass('new-app-page');

            $scope.newAppModalTitle = "Welcome to hyprMediate!";
            $scope.newAppPage = true;
            $scope.systemMessage = "Your confirmation email will arrive shortly.";
            $scope.newApp = {appName: null, currencyName: null, rewardMin: null, rewardMax: null, roundUp: true};

            // Submit form if fields are valid.
            $scope.submitNewApp = function(form) {
                $scope.errors = {};
                if(form.$valid) {
                    var parsedRewardMax = parseInt($scope.newApp.rewardMax);
                    $scope.newApp.rewardMax = isNaN(parsedRewardMax) ? null : parsedRewardMax;
                    $scope.newApp.rewardMin = parseInt($scope.newApp.rewardMin);
                    $scope.newApp.exchangeRate = parseInt($scope.newApp.exchangeRate);
                    $http.post('/distributors/' + $routeParams.distributorID + '/apps', $scope.newApp).
                        success(function(data, status, headers, config) {
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
        }]
);

