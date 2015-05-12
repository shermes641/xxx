appsControllers.controller( 'NewAppsController', [ '$scope', '$window', '$http', '$routeParams', 'flashMessage',
        function( $scope, $window, $http, $routeParams, flashMessage ) {
            $('body').addClass('new-app-page');

            $scope.newAppModalTitle = "Welcome to hyprMediate!";
            $scope.newAppPage = true;
            $scope.newApp = {appName: null, currencyName: null, rewardMin: null, rewardMax: null, roundUp: true};
            $scope.flashMessage = flashMessage;
            flashMessage.add({message: "Your confirmation email will arrive shortly.", status: "success"});

            // Submit form if fields are valid.
            $scope.submitNewApp = function(form) {
                ga('send', 'event', 'new_app_submit', 'click', 'newapp');
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
                                flashMessage.add(data);
                            }
                        });
                }
            };
        }]
);

