distributorUsersControllers.controller('SignupController', ['$scope', '$http', '$routeParams', '$window',
        function($scope, $http, $routeParams, $window) {
            $scope.showTerms = false;
            $scope.waitForAuth = false;
            $scope.errors = {};
            $scope.termsTemplate = 'assets/templates/distributor_users/terms.html';

            $scope.toggleTerms = function() {
                $scope.showTerms = !$scope.showTerms;
            };

            // Submit form if fields are valid.
            $scope.submit = function(form) {
                $scope.errors = {};
                if(form.$valid) {
                    $scope.waitForAuth = true;
                    $http.post('/distributor_users', $scope.data).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/distributors/"+data.distributorID+"/apps/new";
                        }).
                        error(function(data, status, headers, config) {
                            $scope.waitForAuth = false;
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
