"use strict";

var requiredFields = [":input[id=company]", ":input[id=email]", ":input[id=password]", ":input[id=confirmation]"];

// Check if the terms checkbox has been checked.
var termsChecked = function() {
    return($(":input[id=terms]").prop("checked"));
};

// Check if all required fields are filled.
var fieldsFilled = function() {
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

// Enables or disables the submit button.
var toggleSubmit = function(disabledStatus) {
    var submitButton = $(':input[name=submit]');
    submitButton.prop("disabled", disabledStatus);
    submitButton.toggleClass('button inactive', disabledStatus);
};

toggleSubmit(true);

$(document).ready(function() {
    $("#viewTerms").click(function(event) {
        $("#termsContainer").toggle();
    });

    $(":input").change(function() {
        if(fieldsFilled() && termsChecked()) {
            toggleSubmit(false);
        } else {
            toggleSubmit(true);
        }
    });
});
