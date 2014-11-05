"use strict";

var requiredFields = [":input[id=appName]", ":input[id=currencyName]", ":input[id=exchangeRate]"];


// Enables or disables the submit button.
var toggleSubmit = function(disabledStatus) {
    var submitButton = $(':input[name=new-app-form]');
    submitButton.prop("disabled", disabledStatus);
    submitButton.toggleClass('button inactive', disabledStatus);
};

toggleSubmit(true);

$(document).ready(function() {
    $(":input").change(function() {
        if(fieldsFilled(requiredFields)) {
            toggleSubmit(false);
        } else {
            toggleSubmit(true);
        }
    });

    // Submit form if fields are valid.
    $(":button[name=new-app-form]").click(function(event) {
        event.preventDefault();
        if(validRewardAmounts()) {
            $("form[name=new-app-form]").submit();
        } else {
            event.preventDefault();
        }
    });
});
