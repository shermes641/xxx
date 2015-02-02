/*
"use strict";

mediationModule.controller( 'AppsController', [ '$scope', '$http',
    function( $scope, $http ) {
        var formSelector = $("#edit-app");
        var distributorID = formSelector.attr("data-distributor-id");
        var appID = formSelector.attr("data-app-id");

        // Submit form if fields are valid.
        $scope.submit = function() {
            debugger;
            if(validRewardAmounts() && validExchangeRate() && validCallback()) {
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

$(document).ready(function() {
    // Submit form if fields are valid.
    $(":button[name=submit]").click(function(event) {
        if(validRewardAmounts() && validExchangeRate() && validCallback()) {
            $("form").submit();
        } else {
            event.preventDefault();
        }
    });
});
*/
