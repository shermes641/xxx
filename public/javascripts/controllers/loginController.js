distributorUsersControllers.controller('LoginController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage',
        function($scope, $http, $routeParams, $window, flashMessage) {
            $scope.invalidForm = true;
            $scope.waitForAuth = false;
            $scope.errors = {};
            $scope.flashMessage = flashMessage;
            if($routeParams.recently_logged_out === "true") {
                flashMessage.add({message: "You are now logged out.", status: "success"});
            }

            // Submit form if fields are valid.
            $scope.submit = function(form) {
                $scope.errors = {};
                if(form.$valid) {
                    $scope.waitForAuth = true;
                    $http.post('/authenticate', $scope.data).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/";
                        }).
                        error(function(data, status, headers, config) {
                            $scope.waitForAuth = false;
                            if(data.fieldName) {
                                $scope.errors[data.fieldName] = data.message;
                                $scope.errors[data.fieldName + "Class"] = "error";
                            }
                        });
                }
            };
        }]
);

