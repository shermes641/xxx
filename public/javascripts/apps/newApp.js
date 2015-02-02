"use strict";

/*
mediationModule.controller( 'AppsController', [ '$scope', '$http', '$routeParams',
    function( $scope, $http, $routeParams ) {
        debugger;
        var requiredFields = [":input[id=appName]", ":input[id=currencyName]", ":input[id=exchangeRate]", ":input[id=rewardMin]"];
        var distributorID = $("#new-app").attr("data-distributor-id");

        $scope.invalidForm = true;
        $scope.inactiveClass = "inactive";

        $scope.checkInputs = function() {
            if(fieldsFilled(requiredFields)) {
                $scope.invalidForm = false;
                $scope.inactiveClass = "";
            } else {
                $scope.invalidForm = true;
                $scope.inactiveClass = "inactive";
            }
        };

        // Submit form if fields are valid.
        $scope.submit = function() {
            if(validRewardAmounts() && validExchangeRate()) {
                $http.post('/distributors/' + distributorID + '/apps', $scope.data).
                    success(function(data, status, headers, config) {
                        flashMessage(data.message, defaultSuccessDiv);
                    }).
                    error(function(data, status, headers, config) {
                        flashMessage(data.message, defaultErrorDiv);
                    });
            }
        };
    }]
);
*/
