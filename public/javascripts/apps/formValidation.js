"use strict";

// Displays form validation errors.
var flashMessage = function(message) {
    var errorDiv = $("#error-message");
    errorDiv.html(message).fadeIn();
    errorDiv.delay(3000).fadeOut("slow");
};

// Checks if Reward Minimum is greater than Reward Maximum.
var validRewardAmounts = function() {
    var rewardMin = $(":input[name=rewardMin]").val();
    var rewardMax = $(":input[name=rewardMax]").val();
    var validResult = true;
    if(rewardMin !== "" && rewardMax !== "") {
        validResult = parseInt(rewardMax) > parseInt(rewardMin);
    }
    return(validResult);
};
