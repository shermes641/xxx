mediationModule.controller( 'AdminController', [ '$scope', '$http', '$routeParams', '$filter', '$timeout', '$location', 'flashMessage', 'sharedIDs', 'filterFilter', '$window', 'platforms',
        function( $scope, $http, $routeParams, $filter, $timeout, $location, flashMessage, sharedIDs, filterFilter, $window, platforms ) {
            $scope.platforms = platforms.all();
            $scope.apps = [];
            $scope.distributor = {};
            $scope.flashMessage = flashMessage;

            $scope.getDistributorData = function() {
                $http.get('admin/distributor_info').success(function(data) {
                    var apps = data.apps;
                    var distributors = data.distributors;
                    apps[0].header = 'Apps';
                    distributors[0].header = 'Distributors';
                    $scope.data = data;
                    $scope.appsAndDistributors = distributors.concat(apps);
                }).error(function(data) {
                });
            };

            $scope.getDistributorData();

            $scope.noDistributorSelected = function() {
                return Object.keys($scope.distributor).length === 0;
            };

            $scope.morph = function() {
                $http.get('admin/morph?distributor_id=' + $scope.distributor.id).success(function(data) {
                    $window.location.href = '/';
                }).error(function(data) {
                    $scope.flashMessage.add(data);
                });
            };

            $scope.updateApp = function(app) {
                $http.post('admin/update_app', app).success(function(data) {
                    $scope.flashMessage.add(data);
                }).error(function(data) {
                    $scope.flashMessage.add(data);
                });
            };

            $scope.invalidRefresh = function(refreshInterval) {
                return refreshInterval === null || typeof(refreshInterval) === 'undefined' || refreshInterval < 0;
            };

            $scope.getDistributorsAndApps = function(search) {
                return filterFilter($scope.appsAndDistributors, {'$': search === " " ? "" : search});
            };

            $scope.selectDistributor = function(distributor) {
                var distributorID = distributor.distributor_id || distributor.id;
                $scope.distributor = $scope.data.distributors.filter(function(e) { return e.id === distributorID; })[0];
                $scope.apps = $scope.data.apps.filter(function(e) { return e.distributor_id === distributorID; });
                $scope.distributorInput = distributor.name;
            };

            $scope.clearFilteredDistributors = function() {
                if($scope.distributorInput === "" || $scope.distributor.name === undefined) {
                    $scope.distributor = {};
                    $scope.apps = [];
                    $scope.distributorInput = "";
                }
            };
        } ]
);
