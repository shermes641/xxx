"use strict";

$(document).ready(function() {
    $("#waterfall-list").sortable();
    $("#waterfall-list").on("sortdeactivate", function(event, ui) {
        postUpdate();
    });

    // Rearranges the waterfall order list either by ecpm or original order.
    var orderList = function(orderAttr, ascending) {
        var newOrder = $("#waterfall-list li").sort(function(li1, li2) {
            return (ascending ? $(li1).attr(orderAttr) - $(li2).attr(orderAttr) : $(li2).attr(orderAttr) - $(li1).attr(orderAttr))
        });
        appendNewList(newOrder);
    }

    // Displays the updated list order in view.
    var appendNewList = function(newOrder) {
        var list = $("#waterfall-list");
        $.each(newOrder, function(index, listItem){
            list.append(listItem);
        })
    }

    // Retrieves ordered list of ad providers who are either active or inactive
    var providersByActive = function(active) {
        return $('#waterfall-list li').filter(function(index, li) { return($(li).attr("data-active") === active) })
    }

    // Updates waterfall properties via AJAX.
    var postUpdate = function() {
        var waterfallID = $('.content').attr("data-waterfall-id");
        var distributorID = $('.content').attr("data-distributor-id");
        var path = "/distributors/" + distributorID + "/waterfalls/" + waterfallID;
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

    // Retrieves current list order and value of waterfall name field.
    var updatedData = function() {
        var adProviderList = providersByActive("true")
        adProviderList.push.apply(adProviderList, providersByActive("false"))
        var order = adProviderList.map(function(index, el) {
            return({
                id: $(this).attr("data-id"),
                newRecord: $(this).attr("data-new-record"),
                active: $(this).attr("data-active"),
                waterfallOrder: index.toString()
            });
        }).get();
        return(JSON.stringify({adProviderOrder: order}));
    }

    // Displays success or error of AJAX request.
    var flashMessage = function(message, div) {
        div.html(message).fadeIn();
        div.delay(3000).fadeOut("sow");
    }

    // Initiates AJAX request to update waterfall.
    $(":button[name=submit]").click(function() {
        postUpdate();
    });

    // Orders waterfall list by ecpm descending.
    $(":button[name=order]").click(function() {
        orderList("data-cpm", false);
        postUpdate();
    });

    // Reverts waterfall list back to the original order from initial page load.
    $(":button[name=reset]").click(function() {
        orderList("data-order-number", true);
    });

    // Controls activation/deactivation of each ad provider in a waterfall.
    $("#waterfall-list li").click(function(event) {
        var buttonID = $(event.target).attr("id");
        var buttonSelect = "#waterfall-list " + ":button[id=" + buttonID + "]";
        var selector = "#waterfall-list [id=" + buttonID + "]";
        var listItem = $(selector);
        var originalVal = listItem.attr("data-active");
        listItem.attr("data-active", (originalVal === "true" ? "false" : "true"));
        listItem.toggleClass("inactive");
        listItem.attr("data-active") === "true" ? $(buttonSelect).html("Deactivate") : $(buttonSelect).html("Activate")
        postUpdate();
    });
});
