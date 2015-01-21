"use strict";

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
