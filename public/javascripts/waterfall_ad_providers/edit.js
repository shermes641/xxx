"use strict";

$(document).ready(function() {
    // Updates WaterfallAdProvider properties via AJAX.
    var postUpdate = function() {
        var waterfallAdProviderID = $('.content').attr("data-waterfall-ad-provider-id");
        var distributorID = $('.content').attr("data-distributor-id");
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
            fieldObj[label] = value;
        }).get();
        return(JSON.stringify(fieldObj));
    }

    // Displays success or error of AJAX request.
    var flashMessage = function(message, div) {
        div.html(message).fadeIn();
        div.delay(3000).fadeOut("sow");
    }

    // Initiates AJAX request to update WaterfallAdProvider.
    $(":button[name=submit]").click(function(event) {
        event.preventDefault();
        postUpdate();
    });
});
