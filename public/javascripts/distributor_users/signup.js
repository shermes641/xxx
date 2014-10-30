"use strict";

var requiredFields = [":input[id=company]", ":input[id=email]", ":input[id=password]", ":input[id=confirmation]"];

// Check if the terms checkbox has been checked.
var termsChecked = function() {
    return($(":input[id=terms]").prop("checked"));
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
        if(fieldsFilled(requiredFields) && termsChecked()) {
            toggleSubmit(false);
        } else {
            toggleSubmit(true);
        }
    });
});
