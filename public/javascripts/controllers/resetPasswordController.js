distributorUsersControllers.controller('ResetPasswordController', ['$scope', '$http', '$routeParams', '$window', 'flashMessage',
        function($scope, $http, $routeParams, $window, flashMessage) {
            $scope.waitForAuth = false;
            $scope.errors = {};
            $scope.flashMessage = flashMessage;
            $scope.formName = 'resetPasswordForm';
            $scope.resetParams = {
                email: $routeParams.email,
                distributor_user_id: parseInt($routeParams.distributor_user_id),
                token: $routeParams.token
            };

            // Submit form if fields are valid.
            $scope.submit = function(form) {
                ga('send', 'event', 'login_submit', 'click', 'login');
                $scope.errors = {};
                if(form.$valid) {
                    $scope.waitForAuth = true;
                    $http.post('/distributor_users/reset_password', _.extend($scope.data, $scope.resetParams)).
                        success(function(data, status, headers, config) {
                            $window.location.href = "/";
                        }).
                        error(function(data, status, headers, config) {
                            $scope.waitForAuth = false;
                            flashMessage.add(data);
                        });
                }
            };
        }]
);
