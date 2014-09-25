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
            flashMessage(result.message, $("#success-message"))
        },
        error: function(result) {
            flashMessage(result.message, $("#error-message"))
        }
    });
}

// Retrieves field names and values for WaterfallAdProvider edit form.
var updatedData = function() {
    var fieldObj = {};
    var fields = $("form[name=edit-waterfall-ad-provider] div[class=edit-waterfall-ad-provider-field]");
    fields.map(function(index, el) {
        var label = $(el).children("label").html();
        var value = $(el).children("input").val();
        var dataType = $(el).find('input').attr("data-param-type");
        if(dataType == "Array") {
            fieldObj[label] = value.split(",").map(function(el, index) { return(el.trim()); });
        } else {
            fieldObj[label] = value;
        }
    }).get();
    return(JSON.stringify(fieldObj));
}

// Displays success or error of AJAX request.
var flashMessage = function(message, div) {
    div.html(message).fadeIn();
    div.delay(3000).fadeOut("slow");
}

// Closes modal window and returns background div class to normal.
var closeModal = function() {
    $(".content.waterfall_list").toggleClass("modal-inactive", false);
    $("#edit-waterfall-ad-provider").dialog("close");
}

$(document).ready(function() {
    // Initiates AJAX request to update WaterfallAdProvider.
    $(":button[name=update-ad-provider]").click(function(event) {
        event.preventDefault();
        postUpdate();
        closeModal();
    });

    // Closes waterfall ad provider editing modal.
    $(":button[name=cancel]").click(function(event) {
        event.preventDefault();
        closeModal();
    });
});
