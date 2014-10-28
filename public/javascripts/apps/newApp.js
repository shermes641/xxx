"use strict";

$("#new-virtual-currency").hide();

$(document).ready(function() {
    // Switch to virtual currency portion of form if App Name is filled out.
    $(":button[name=app-form-next]").click(function(event) {
        event.preventDefault();
        if($(":input[name=appName]").val() === "") {
            flashMessage("App Name is required");
        } else {
            $("#new-app").hide();
            $("#new-virtual-currency").show();
        }
    });

    // Submit form if fields are valid.
    $(":button[name=virtual-currency-form]").click(function(event) {
        event.preventDefault();
        if(validRewardAmounts() && formComplete()) {
            $("form[name=new-app-form]").submit();
        } else {
            event.preventDefault();
        }
    });

    // Switch back to app portion of form.
    $(":button[name=virtual-currency-form-back]").click(function(event) {
        event.preventDefault();
        $("#new-virtual-currency").hide();
        $("#new-app").show();
    });
});
