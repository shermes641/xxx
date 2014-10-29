"use strict";

$(document).ready(function() {
    // Submit form if fields are valid.
    $(":button[name=submit]").click(function(event) {
        if(!validRewardAmounts()) {
            event.preventDefault();
            flashMessage(rewardAmountErrorMessage);
        } else if(!validCallback()) {
            event.preventDefault();
            flashMessage(callbackErrorMessage);
        } else {
            $("form").submit();
        }
    });
});
