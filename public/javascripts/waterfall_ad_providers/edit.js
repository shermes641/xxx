"use strict";

// Updates WaterfallAdProvider properties via AJAX.
var postWAPUpdate = function() {
    var waterfallAdProviderID = $('.contents').attr("data-waterfall-ad-provider-id");
    var distributorID = $('.contents').attr("data-distributor-id");
    var path = "/distributors/" + distributorID + "/waterfall_ad_providers/" + waterfallAdProviderID;
    var providerName = $(".contents").attr("data-provider-name");
    $.ajax({
        url: path,
        type: 'POST',
        dataType: "json",
        contentType: "application/json",
        data: updatedWAPData(),
        success: function(result) {
            var listItem = $("li[data-id=" + waterfallAdProviderID + "]");
            var params = changedAppRestartParams();
            var newEcpm = JSON.parse(updatedWAPData())["eCPM"];
            $(".content").attr("data-generation-number", result.newGenerationNumber);
            if(params.length > 0) {
                var paramMessage = params.length == 1 ? params[0] : params.slice(0, params.length-1).join(", ") + ", and " + params[params.length-1];
                var message = "You changed " + paramMessage + " for " + providerName + ". Your App must be restarted for this change to take effect.";
                flashMessage(message, waterfallSuccessDiv);
            } else {
                flashMessage(result.message, waterfallSuccessDiv);
            }
            clearAppRestartParams();
            listItem.children(".hideable").children("div").children("div[name=cpm]").html(newEcpm);
            listItem.attr("data-cpm", newEcpm);
            if(optimizeToggleButton.is(":checked")) {
                orderList("data-cpm", false);
            }
            if(listItem.attr("data-unconfigured")) {
                listItem.attr("data-unconfigured", "false");
                listItem.children().children("button[name=status]").removeClass("undisplayed-wap-info");
            }
        },
        error: function(result) {
            flashMessage(result.responseJSON.message, waterfallErrorDiv);
            clearAppRestartParams();
        }
    });
};

// Retrieves labels and values for configuration fields.
var retrieveFields = function(fieldType) {
    var fields = $("form[name=edit-waterfall-ad-provider] div[class=" + fieldType + "]");
    return(fields.toArray().reduce(function(fieldObj, el, index) {
        var label = $(el).children("label").attr("data-param-name");
        var value = $(el).children("input").val();
        var dataType = $(el).find('input').attr("data-param-type");
        if(dataType == "Array") {
            fieldObj[label] = value.split(",").map(function(el, index) { return(el.trim()); });
        } else {
            fieldObj[label] = value;
        }
        return fieldObj;
    }, {}))
};

// Assembles JSON to be saved as configuration_data in the waterfall_ad_providers table.
var updatedWAPData = function() {
    return(JSON.stringify(
        {
            configurationData: {
                requiredParams: retrieveFields("edit-waterfall-ad-provider-field"), reportingParams: retrieveFields("reporting-waterfall-ad-provider-field"),
                callbackParams: retrieveFields("callback-waterfall-ad-provider-field")
            },
            reportingActive: reportingActiveToggleButton.prop("checked").toString(),
            appToken: appToken,
            waterfallID: waterfallID,
            eCPM: $(":input[name=eCPM]").val(),
            generationNumber: $(".content").attr("data-generation-number")
        }
    ));
};

// Closes modal window and returns background div class to normal.
var closeModal = function() {
    $(".content.waterfall_list").toggleClass("modal-inactive", false);
    $("#edit-waterfall-ad-provider").dialog("close");
};

// Selector for reporting active toggle.
var reportingActiveToggleButton = $(":checkbox[name=reporting-active]");

// Prevents tracking of previously changed params after a user has either submitted or cancelled the changes.
var clearAppRestartParams = function() {
    appRestartParams = {};
};

// Returns all params which have been changed and require an app restart to take effect.
var changedAppRestartParams = function() {
    return(Object.keys(appRestartParams));
};

// Input field for eCPM.
var eCPMInput = $(":input[name=eCPM]");

// Enables and disables input field for eCPM.
var toggleConfigurableEcpm = function() {
    var reportingStatus = reportingActiveToggleButton.is(":checked");
    eCPMInput.attr("disabled", reportingStatus);
    eCPMInput.toggleClass("inactive-input");
};

// Checks eCPM input is a number.
var validEcpm = function() {
    if($.isNumeric(eCPMInput.val())) {
        return true;
    } else {
        flashMessage("eCPM must be a valid number.", waterfallErrorDiv);
        return false
    }
};

// Checks if fields are filled out for a WaterfallAdProvider and flashes error message if not.
var validDynamicFields = function(requiredFields) {
   for(var i = 0; i < requiredFields.length; i++) {
       var field = requiredFields[i];
       var input = $.trim($(field).children(":input").val());
       if(!input.length > 0) {
           var paramName = $(field).children("label").html().replace("*", "");
           flashMessage(paramName + " is required.", waterfallErrorDiv);
           return false;
       }
   }
   return true;
};

if(reportingActiveToggleButton.is(":checked")) {
    eCPMInput.attr("disabled", true);
    eCPMInput.addClass("inactive-input");
}

$(document).ready(function() {
    var requiredDynamicFields = $(".edit-waterfall-ad-provider-field[data-required-param=true]");

    // Initiates AJAX request to update WaterfallAdProvider.
    $(":button[name=update-ad-provider]").click(function(event) {
        event.preventDefault();
        if(validEcpm() && validDynamicFields(requiredDynamicFields)) {
            postWAPUpdate();
            closeModal();
        }
    });

    // Closes waterfall ad provider editing modal.
    $(".waterfall_list").click(function() {
        closeModal();
    });


    // Closes waterfall ad provider editing modal.
    $(":button[name=cancel]").click(function(event) {
        event.preventDefault();
        closeModal();
        clearAppRestartParams();
    });

    reportingActiveToggleButton.click(function(event) {
        if(validEcpm()) {
            postWAPUpdate();
            toggleConfigurableEcpm();
        } else {
            event.preventDefault();
        }
    });
});
