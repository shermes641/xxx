distributorUsersControllers.controller('LoginController', ['$scope', '$http', '$routeParams', '$window', 'fieldsFilled',
        function($scope, $http, $routeParams, $window, fieldsFilled) {
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
                            }
                        });
                }
            };
        }]
);

