mediationModule.controller( 'RoleManagementController', [ '$scope', '$http', '$routeParams', '$filter', '$timeout', '$location', 'flashMessage', 'sharedIDs', 'filterFilter', '$window',
        function( $scope, $http, $routeParams, $filter, $timeout, $location, flashMessage, sharedIDs, filterFilter, $window ) {
            $scope.flashMessage = flashMessage;
            $scope.availableRoles = [];
            $scope.existingRoles = [];
            $scope.userID = null;

            $scope.getRoleData = function() {
                $http.get('admin/role_info').success(function(data) {
                    $scope.data = data;
                }).error(function(data) {
                    flashMessage.add(data);
                });
            };

            $scope.getRoleData();

            $scope.removeRole = function(role) {
                $http.post('admin/roles/delete', role).success(function(data) {
                    var roleIndex = $scope.existingRoles.findIndex(function(e) {return e === role;});
                    $scope.existingRoles.splice(roleIndex, 1);
                    setAvailableRoles();
                    $scope.flashMessage.add(data);
                }).error(function(data) {
                    $scope.flashMessage.add(data);
                });
            };

            var setAvailableRoles = function() {
                var roleNames = $scope.existingRoles.map(function(e) { return e.name; });
                $scope.availableRoles = $scope.data.roles.filter(function(role) { return !_.contains(roleNames, role.name); });
            };

            $scope.addRole = function(role) {
                var roleData = {distributor_user_id: $scope.userID, role_id: role.id};
                $http.post('admin/roles', roleData).success(function(data) {
                    var roleIndex = $scope.availableRoles.findIndex(function(e) {return e === role;});
                    $scope.availableRoles.splice(roleIndex, 1);
                    $scope.existingRoles.push(data.user_role);
                    $scope.flashMessage.add(data);
                }).error(function(data) {
                    $scope.flashMessage.add(data);
                });
            };

            $scope.getUsers = function(search) {
                var users = $scope.data.distributor_users.reduce(function(allUsers, user) {
                    if(allUsers.filter(function(e) { return e.email === user.email; }).length === 0) {
                        return allUsers.concat(user);
                    } else {
                        return allUsers;
                    }
                }, []);
                return filterFilter(users, search === " " ? "" : search);
            };

            $scope.selectUser = function(user) {
                $scope.userID = user.distributor_user_id;
                $scope.existingRoles = $scope.data.distributor_users.filter(function(role) {
                    return role.role_id !== null && role.email === user.email;
                });
                setAvailableRoles();
                $scope.userInput = user.email;
            };

            $scope.clearFilteredUsers = function() {
                if($scope.userInput === "" || $scope.userID === null) {
                    $scope.availableRoles = [];
                    $scope.existingRoles = [];
                    $scope.userID = null;
                }
            };
        } ]
);
