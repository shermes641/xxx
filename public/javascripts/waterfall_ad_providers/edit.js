"use strict";

// Updates WaterfallAdProvider properties via AJAX.
var postUpdate = function() {
    var waterfallAdProviderID = $('.contents').attr("data-waterfall-ad-provider-id");
    var distributorID = $('.contents').attr("data-distributor-id");
    var path = "/distributors/" + distributorID + "/waterfall_ad_providers/" + waterfallAdProviderID;
    $.ajax({
        url: path,
        type: 'POST',
        dataType: "json",
        contentType: "application/json",
        data: updatedData(),
        success: function(result) {
            flashMessage(result.message, $("#wap-edit-success"))
        },
        error: function(result) {
            flashMessage(result.message, $("#wap-edit-error"))
        }
    });
};

// Retrieves labels and values for configuration fields.
var retrieveFields = function(fieldType) {
    var fields = $("form[name=edit-waterfall-ad-provider] div[class=" + fieldType + "]");
    return(fields.toArray().reduce(function(fieldObj, el, index) {
        var label = $(el).children("label").html();
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
var updatedData = function() {
    return(JSON.stringify(
        {
            configurationData: {
                requiredParams: retrieveFields("edit-waterfall-ad-provider-field"), reportingParams: retrieveFields("reporting-waterfall-ad-provider-field"),
                callbackParams: retrieveFields("callback-waterfall-ad-provider-field")
            },
            reportingActive: reportingActiveToggleButton.prop("checked").toString()
        }
    ));
};

// Displays success or error of AJAX request.
var flashMessage = function(message, div) {
    div.html(message).fadeIn();
    div.delay(3000).fadeOut("slow");
};

// Closes modal window and returns background div class to normal.
var closeModal = function() {
    $(".content.waterfall_list").toggleClass("modal-inactive", false);
    $("#edit-waterfall-ad-provider").dialog("close");
};

// Selector for reporting active toggle.
var reportingActiveToggleButton = $(":checkbox[name=reporting-active]");

$(document).ready(function() {
    // Initiates AJAX request to update WaterfallAdProvider.
    $(":button[name=update-ad-provider]").click(function(event) {
        event.preventDefault();
        postUpdate();
        closeModal();
    });

    // Closes waterfall ad provider editing modal.
    $(".waterfall_list").click(function() {
        closeModal();
    });


    // Closes waterfall ad provider editing modal.
    $(":button[name=cancel]").click(function(event) {
        event.preventDefault();
        closeModal();
    });

    reportingActiveToggleButton.click(function(event) {
        postUpdate();
    });
});
