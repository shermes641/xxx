"use strict";

// Rearranges the waterfall order list either by eCPM or original order.
var orderList = function(orderAttr, ascending) {
    var newOrder = $("#waterfall-list li").sort(function(li1, li2) {
        return (ascending ? $(li1).attr(orderAttr) - $(li2).attr(orderAttr) : $(li2).attr(orderAttr) - $(li1).attr(orderAttr))
    });
    appendNewList(newOrder);
};

// Displays the updated list order in view.
var appendNewList = function(newOrder) {
    var list = $("#waterfall-list");
    $.each(newOrder, function(index, listItem){
        list.append(listItem);
    })
};

// Retrieves ordered list of ad providers who are either active or inactive
var providersByActive = function(active) {
    return $('#waterfall-list li').filter(function(index, li) { return($(li).attr("data-active") === active) });
};

// Updates waterfall properties via AJAX.
var postUpdate = function() {
    var waterfallID = $('.content').attr("data-waterfall-id");
    var distributorID = $('.content').attr("data-distributor-id");
    var path = "/distributors/" + distributorID + "/waterfalls/" + waterfallID;
    $.ajax({
        url: path,
        type: 'POST',
        contentType: "application/json",
        data: updatedData(),
        success: function(result) {
            flashMessage(result.message, $("#success-message"));
        },
        error: function(result) {
            flashMessage(result.message, $("#error-message"));
        }
    });
};

// Creates waterfall ad provider via AJAX.
var createWaterfallAdProvider = function(adProviderID) {
    var waterfallID = $('.content').attr("data-waterfall-id");
    var distributorID = $('.content').attr("data-distributor-id");
    var path = "/distributors/" + distributorID + "/waterfall_ad_providers";
    $.ajax({
        url: path,
        type: 'POST',
        contentType: "application/json",
        data: JSON.stringify({waterfallID: waterfallID, adProviderID: adProviderID}),
        success: function(result) {
            var item = $("li[id=true-" + adProviderID + "]");
            var configureButton = item.find("button[name=configure-wap]");
            item.attr("data-new-record", "false");
            item.attr("id", "false-" + result.wapID);
            item.attr("data-id", result.wapID);
            configureButton.attr("data-waterfall-ad-provider-id", result.wapID);
            configureButton.show();
            postUpdate();
        },
        error: function(result) {
            flashMessage(result.message, $("#error-message"));
        }
    });
};

// Retrieves current list order and value of waterfall name field.
var updatedData = function() {
    var adProviderList = providersByActive("true");
    var optimizedOrder = optimizeToggleButton.prop("checked").toString();
    var testMode = testModeButton.prop("checked").toString();
    adProviderList.push.apply(adProviderList, providersByActive("false").length > 0 ? providersByActive("false") : [])
    var order = adProviderList.map(function(index, el) {
        return({
            id: $(this).attr("data-id"),
            newRecord: $(this).attr("data-new-record"),
            active: $(this).attr("data-active"),
            waterfallOrder: index.toString()
        });
    }).get();
    return(JSON.stringify({adProviderOrder: order, optimizedOrder: optimizedOrder, testMode: testMode}));
};

// Displays success or error of AJAX request.
var flashMessage = function(message, div) {
    div.html(message).fadeIn();
    div.delay(3000).fadeOut("sow");
};

// Selector for button which toggles waterfall optimization.
var optimizeToggleButton = $(":checkbox[name=optimized-order]");

// Selector for button which toggles waterfall from live to test mode.
var testModeButton = $(":checkbox[name=test-mode]");

$("#waterfall-list").sortable({containment: ".content"});
$("#edit-waterfall-ad-provider").dialog({modal: true, autoOpen: false});

// Disable sortable list if waterfall has optimized_order set to true.
$("#waterfall-list").sortable(optimizeToggleButton.prop("checked") ? "disable" : "enable");

$(document).ready(function() {
    // Initiates AJAX request to update waterfall.
    $(":button[name=submit]").click(function() {
        postUpdate();
    });

    // Controls activation/deactivation of each ad provider in a waterfall.
    $("button[name=status]").click(function(event) {
        var button = $(event.target);
        var listItem = button.parent();
        var originalVal = listItem.attr("data-active");
        listItem.attr("data-active", (originalVal === "true" ? "false" : "true"));
        listItem.toggleClass("inactive");
        listItem.attr("data-active") === "true" ? button.html("Deactivate") : button.html("Activate");
        if(listItem.attr("data-new-record") === "true") {
            createWaterfallAdProvider(listItem.attr("data-id"));
        } else {
            postUpdate();
        }
    });

    // Opens modal for editing waterfall ad provider configuration info.
    $(":button[name=configure-wap]").click(function(event) {
        var distributorID = $(event.target).attr("data-distributor-id");
        var waterfallAdProviderID = $(event.target).attr("data-waterfall-ad-provider-id");
        var path = "/distributors/" + distributorID + "/waterfall_ad_providers/" + waterfallAdProviderID + "/edit";
        $(".content.waterfall_list").toggleClass("modal-inactive", true);
        $.ajax({
            url: path,
            type: 'GET',
            success: function(data) {
                $("#edit-waterfall-ad-provider").html(data).dialog({modal: true}).dialog("open");
                $(".ui-dialog-titlebar").hide();
            },
            error: function(data) {
                flashMessage(data.message, $("#error-message"));
            }
        });
    });

    // Click event for when Optimized Mode is toggled.
    optimizeToggleButton.click(function() {
        var sortableOption;
        if(optimizeToggleButton.prop("checked")) {
            sortableOption = "disable";
            orderList("data-cpm", false);
        } else {
            sortableOption = "enable";
        }
        $("#waterfall-list").sortable(sortableOption);
        postUpdate();
    });

    // Click event for when Test Mode is toggled.
    testModeButton.click(function(event) {
        var newRecords = $('#waterfall-list li').filter(function(index, li) { return($(li).attr("data-new-record") === "true") }).length;
        var allRecords = $('#waterfall-list li').length;
        // Prevent the user from setting the waterfall to live without configuring any ad providers.
        if(newRecords == allRecords) {
            event.preventDefault();
            flashMessage("You must activate an ad provider before the waterfall can go live.", $("#error-message"));
        } else {
            postUpdate();
        }
    });

    // Sends AJAX request when waterfall order is changed via drag and drop.
    $("#waterfall-list").on("sortdeactivate", function(event, ui) {
        postUpdate();
    });
});
