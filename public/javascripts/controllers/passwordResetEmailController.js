distributorUsersControllers.controller('PasswordResetEmailController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage',
        function($scope, $http, $routeParams, $window, flashMessage) {
            $scope.waitForAuth = false;
            $scope.errors = {};
            $scope.flashMessage = flashMessage;
            $scope.formName = 'passwordResetEmailForm';

            // Submit form if fields are valid.
            $scope.submit = function(form) {
                ga('send', 'event', 'login_submit', 'click', 'login');
                $scope.errors = {};
                if(form.$valid) {
                    $scope.waitForAuth = true;
                    $http.post('/distributor_users/send_password_reset_email', $scope.data).
                        success(function(data, status, headers, config) {
                            flashMessage.add(data);
                            $scope.data.email = null;
                            form.$setPristine();
                            form.$setUntouched();
                            $scope.waitForAuth = false;
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
