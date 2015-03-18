distributorUsersControllers.controller('SignupController', ['$scope', '$http', '$routeParams', '$window', 'fieldsFilled',
        function($scope, $http, $routeParams, $window, fieldsFilled) {
            $scope.showTerms = false;
            $scope.invalidForm = true;
            $scope.waitForAuth = false;
            $scope.inactiveClass = "inactive";
            $scope.errors = {};
            $scope.termsTemplate = 'assets/templates/distributor_users/terms.html';

            $scope.toggleTerms = function() {
                $scope.showTerms = !$scope.showTerms;
            };

            $scope.checkInputs = function() {
                var requiredFields = ['company', 'email', 'password', 'confirmation'];
                if(fieldsFilled($scope.data, requiredFields) && $scope.data.terms) {
                    $scope.invalidForm = false;
                    $scope.inactiveClass = "";
                } else {
                    $scope.invalidForm = true;
                    $scope.inactiveClass = "inactive";
                }
            };

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
                    $scope.waitForAuth = true;
                    $scope.inactiveClass = "inactive";
                    $http.post('/distributor_users', $scope.data).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/distributors/"+data.distributorID+"/apps/new";
                        }).
                        error(function(data, status, headers, config) {
                            $scope.waitForAuth = false;
                            $scope.inactiveClass = "";
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

