"use strict";

// Displays form validation errors.
var flashMessage = function(message) {
    var errorDiv = $("#error-message");
    errorDiv.html(message).fadeIn();
    errorDiv.delay(3000).fadeOut("slow");
};

// Checks if all required fields are filled out.
var formComplete = function() {
    var inputs = [{selector: ":input[name=currencyName]", fieldName: "Currency Name"}, {selector: ":input[name=exchangeRate]", fieldName: "Exchange Rate"}];
    for(var i=0; i < inputs.length; i++) {
        if($(inputs[i].selector).val() === "") {
            flashMessage(inputs[i].fieldName + " required.");
            return false;
        }
    }
    return true;
};

// Checks if Reward Minimum is greater than Reward Maximum.
var validRewardAmounts = function() {
    var rewardMin = $(":input[name=rewardMin]").val();
    var rewardMax = $(":input[name=rewardMax]").val();
    if(typeof rewardMin === "string" && rewardMin !== "" && typeof rewardMax === "string" && rewardMax !== "") {
        if(parseInt(rewardMax) < parseInt(rewardMin)) {
            flashMessage("Reward Maximum must be greater than or equal to Reward Minimum.");
            return false;
        }
    }
    return true;
};

// Checks for a valid callback URL when server to server callbacks are enabled.
var validCallback = function() {
    var callbacksEnabled = $("input[id=serverToServerEnabled_1]").is(":checked");
    var callbackURL = $("input[id=callbackURL]").val();
    if(callbacksEnabled) {
        if(!(/(http|https):\/\//).test(callbackURL)) {
            flashMessage("A valid HTTP or HTTPS callback URL is required.");
            return false;
        }
    }
    return true;
};
