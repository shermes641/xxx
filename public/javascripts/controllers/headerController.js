mediationModule.controller( 'HeaderController', [ '$scope', 'sharedIDs',
    function($scope, sharedIDs) {
        $scope.sharedIDs = sharedIDs;
        $scope.analyticsURL = function() {
            return '/distributors/' + sharedIDs.distributorID() + '/analytics' + (sharedIDs.appID() === undefined ? '' : '?app_id=' + sharedIDs.appID());
        };
    }
]);