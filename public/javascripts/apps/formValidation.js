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
        validResult = parseInt(rewardMax) >= parseInt(rewardMin);
    }
    return(validResult);
};

// Checks for a valid callback URL when server to server callbacks are enabled.
var validCallback = function() {
    var callbacksEnabled = $("input[id=serverToServerEnabled_1]").is(":checked");
    var callbackURL = $("input[id=callbackURL]").val();
    if(callbacksEnabled) {
        return (/(http|https):\/\//).test(callbackURL);
    }
    return true;
};

// Error message displayed when validRewardAmounts is false.
var rewardAmountErrorMessage = "Reward Maximum must be greater than or equal to Reward Minimum.";

// Error message displayed when a callback URL is invalid.
var callbackErrorMessage = "A valid HTTP or HTTPS callback URL is required.";
