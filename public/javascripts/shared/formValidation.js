"use strict";

// Default div for error messages.
var defaultErrorDiv = $("#error-message");

// Displays success or error flash messages.
var flashMessage = function(message, div) {
    div.html(message).fadeIn();
    div.delay(3000).fadeOut("slow");
};

// Check if all required fields are filled.
var fieldsFilled = function(requiredFields) {
    for(var i=0; i < requiredFields.length; i++) {
        var field = $(requiredFields[i]);
        if(typeof field.val() === "string") {
            if(field.val() === "") {
                return false;
            }
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
            flashMessage("Reward Maximum must be greater than or equal to Reward Minimum.", defaultErrorDiv);
            return false;
        }
    }
    return true;
};

// Checks for a valid callback URL when server to server callbacks are enabled.
var validCallback = function() {
    var callbacksEnabled = $(":input[id=serverToServerEnabled]").prop("checked");
    var callbackURL = $(":input[id=callbackURL]").val();
    if(callbacksEnabled) {
        if(!(/(http|https):\/\//).test(callbackURL)) {
            flashMessage("A valid HTTP or HTTPS callback URL is required.", defaultErrorDiv);
            return false;
        }
    }
    return true;
};
