"use strict";


mediationModule.controller( 'WaterfallController', [ '$scope',
    function( $scope ) {
        // Distributor ID to be used in AJAX calls.
        $scope.distributorID = $(".content").attr("data-distributor-id");

        // Waterfall ID to be used in AJAX calls.
        $scope.waterfallID = $('.content').attr("data-waterfall-id");

        // App Token to be used in AJAX calls.
        $scope.appToken = $(".app-token").attr("data-app-token");

        // Selector for button which toggles waterfall optimization.
        $scope.optimizeToggleButton = $(":checkbox[name=optimized-order]");

        // Selector for button which toggles waterfall from live to test mode.
        $scope.testModeButton = $(":checkbox[name=test-mode]");

        // Drop down menu to select the desired waterfall edit page.
        $scope.waterfallSelection = $(":input[id=waterfall-selection]");

        // Stores params that have been changed which require an app restart.
        $scope.appRestartParams = {};

        // Rearranges the waterfall order list either by eCPM or original order.
        $scope.orderList = function(orderAttr, ascending) {
            var newOrder = $scope.providersByActive("true").sort(function(li1, li2) {
                return (ascending ? Number($(li1).attr(orderAttr)) - Number($(li2).attr(orderAttr)) : Number($(li2).attr(orderAttr)) - Number($(li1).attr(orderAttr)))
            });
            var inactive = $scope.providersByActive("false");
            newOrder.push.apply(newOrder, inactive);
            $scope.appendNewList(newOrder);
        };

        // Displays the updated list order in view.
        $scope.appendNewList = function(newOrder) {
            var list = $("#waterfall-list");
            $.each(newOrder, function(index, listItem){
                list.append(listItem);
            })
        };

        // Retrieves ordered list of ad providers who are either active or inactive
        $scope.providersByActive = function(active) {
            return $("#waterfall-list").children("li").filter(function(index, li) { return($(li).attr("data-active") === active) });
        };

        // Updates waterfall properties via AJAX.
        $scope.postUpdate = function() {
            var path = "/distributors/" + distributorID + "/waterfalls/" + waterfallID;
            $.ajax({
                url: path,
                type: 'POST',
                contentType: "application/json",
                data: updatedData(),
                success: function(result) {
                    $(".content").attr("data-generation-number", result.newGenerationNumber);
                    flashMessage(result.message, $("#waterfall-edit-success"));
                    if(optimizeToggleButton.is(":checked")) {
                        $scope.orderList("data-cpm", false);
                    }
                },
                error: function(result) {
                    $scope.flashMessage(result.responseJSON.message, $("#waterfall-edit-error"));
                }
            });
        };

        // Retrieves configuration data for a waterfall ad provider.
        $scope.retrieveConfigData = function(waterfallAdProviderID, newRecord) {
            var path = "/distributors/" + distributorID + "/waterfall_ad_providers/" + waterfallAdProviderID + "/edit";
            $(".content.waterfall_list").toggleClass("modal-inactive", true);
            $.ajax({
                url: path,
                data: {app_token: appToken},
                type: 'GET',
                success: function(data) {
                    $("#edit-waterfall-ad-provider").html(data).dialog({
                        modal: true,
                        open: function() {
                            $(".content.waterfall_list").addClass("unclickable");
                            $("#modal-overlay").toggle();
                        },
                        close: function() {
                            $(".content.waterfall_list").removeClass("unclickable");
                            $("#modal-overlay").toggle();
                        }
                    }).dialog("open");
                    if(!newRecord) {
                        setRefreshOnRestartListeners();
                    }
                    $(".ui-dialog-titlebar").hide();
                },
                error: function(data) {
                    flashMessage(data.responseJSON.message, $("#waterfall-edit-error"));
                }
            });
        };

        // Creates waterfall ad provider via AJAX.
        $scope.createWaterfallAdProvider = function(params, newRecord) {
            var path = "/distributors/" + distributorID + "/waterfall_ad_providers";
            var generationNumber = $(".content").attr("data-generation-number");
            params["waterfallID"] = waterfallID;
            params["appToken"] = appToken;
            params["waterfallOrder"] = "";
            params["generationNumber"] = generationNumber;
            $.ajax({
                url: path,
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(params),
                success: function(result) {
                    var item = $("li[id=true-" + params["adProviderID"] + "]");
                    var configureButton = item.find("button[name=configure-wap]");
                    item.attr("data-new-record", "false");
                    item.attr("id", "false-" + result.wapID);
                    item.attr("data-id", result.wapID);
                    configureButton.show();
                    if(newRecord) {
                        retrieveConfigData(result.wapID, newRecord);
                    }
                    $(".content").attr("data-generation-number", result.newGenerationNumber)
                    flashMessage(result.message, $("#waterfall-edit-success"))
                },
                error: function(result) {
                    flashMessage(result.message, $("#waterfall-edit-error"));
                }
            });
        };

        // Retrieves current list order and value of waterfall name field.
        $scope.updatedData = function() {
            var adProviderList = $scope.providersByActive("true");
            var optimizedOrder = optimizeToggleButton.prop("checked").toString();
            var testMode = testModeButton.prop("checked").toString();
            var generationNumber = $(".content").attr("data-generation-number");
            adProviderList.push.apply(adProviderList, $scope.providersByActive("false").length > 0 ? $scope.providersByActive("false") : []);
            var order = adProviderList.map(function(index, el) {
                return({
                    id: $(this).attr("data-id"),
                    newRecord: $(this).attr("data-new-record"),
                    active: $(this).attr("data-active"),
                    waterfallOrder: index.toString(),
                    cpm: $(this).attr("data-cpm"),
                    configurable: $(this).attr("data-configurable"),
                    pending: $(this).attr("data-pending")
                });
            }).get();
            return(JSON.stringify({adProviderOrder: order, optimizedOrder: optimizedOrder, testMode: testMode, appToken: appToken, generationNumber: generationNumber}));
        };

        // Displays success or error of AJAX request.
        $scope.flashMessage = function(message, div) {
            div.html(message).fadeIn();
            div.delay(3000).fadeOut("slow");
        };

        if( $scope.optimizeToggleButton.prop("checked") ) {
            // Order ad providers by eCPM, disable sortable list, and hide draggable icon if waterfall has optimized_order set to true.
            $scope.orderList( "data-cpm", false );
            $( "#waterfall-list" ).sortable( "disable" );
            $( ".waterfall-drag" ).hide();
        } else {
            // Enable sortable list if waterfall has optimized_order set to true.
            $( "#waterfall-list" ).sortable( "enable" );
        }

        // Adds event listeners to appropriate inputs when the WaterfallAdProvider edit modal pops up.
        $scope.setRefreshOnRestartListeners = function() {
            $( ":input[data-refresh-on-app-restart=true]" ).change( function( event ) {
                var param = $( event.target.parentElement ).children( "label" ).html();
                appRestartParams[ param ] = true;
            } );
        };

        // Initiates AJAX request to update waterfall.
        $( ":button[name=submit]" ).click( function() {
            $scope.postUpdate();
        } );

        // Direct the user to the selected Waterfall edit page.
        $scope.waterfallSelection.change(function() {
            window.location.href = waterfallSelection.val();
        });

        // Controls activation/deactivation of each ad provider in a waterfall.
        $("button[name=status]").click(function(event) {
            var listItem = $(event.target).parents("li");
            var originalVal = listItem.attr("data-active") === "true";
            listItem.children(".hideable").toggleClass("hidden-wap-info", originalVal);
            listItem.attr("data-active", (!originalVal).toString());
            listItem.toggleClass("inactive");
            if(listItem.attr("data-new-record") === "true") {
                $scope.createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: true});
            } else {
                $scope.postUpdate();
            }
        });

        // Opens modal for editing waterfall ad provider configuration info.
        $(":button[name=configure-wap]").click(function(event) {
            var listItem = $(event.target).parents("li");
            var waterfallAdProviderID = listItem.attr("data-id");
            if(listItem.attr("data-new-record") === "true") {
                $scope.createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: false}, true);
            } else {
                $scope.retrieveConfigData(waterfallAdProviderID);
            }
        });

        // Click event for when Optimized Mode is toggled.
        $scope.optimizeToggleButton.click(function() {
            $(".waterfall-drag").toggle();
            var sortableOption;
            if(optimizeToggleButton.prop("checked")) {
                sortableOption = "disable";
                $scope.orderList("data-cpm", false);
            } else {
                sortableOption = "enable";
            }
            $("#waterfall-list").sortable(sortableOption);
            $scope.postUpdate();
        });

        // Click event for when Test Mode is toggled.
        $scope.testModeButton.click(function(event) {
            var waterfallListItems = $("#waterfall-list").children("li");
            var newRecords = waterfallListItems.filter(function(index, li) { return($(li).attr("data-new-record") === "true") }).length;
            var allRecords = waterfallListItems.length;
            // Prevent the user from setting the waterfall to live without configuring any ad providers.
            if(newRecords == allRecords) {
                event.preventDefault();
                $scope.flashMessage("You must activate an ad provider before the waterfall can go live.", $("#waterfall-edit-error"));
            } else {
                $scope.postUpdate();
            }
        });

        // Sends AJAX request when waterfall order is changed via drag and drop.
        $("#waterfall-list").on("sortdeactivate", function(event, ui) {
            $scope.postUpdate();
        });
    } ]
);
//
//
//$(".content").attr("data-generation-number", result.newGenerationNumber);
//flashMessage(result.message, waterfallSuccessDiv)
//},
//error: function(result) {
//    flashMessage(result.message, waterfallErrorDiv);
//}
//});
//};
//
//// Retrieves current list order and value of waterfall name field.
//var updatedWaterfallData = function() {
//    var adProviderList = providersByActive("true");
//    var optimizedOrder = optimizeToggleButton.prop("checked").toString();
//    var testMode = testModeButton.prop("checked").toString();
//    var generationNumber = $(".content").attr("data-generation-number");
//    adProviderList.push.apply(adProviderList, providersByActive("false").length > 0 ? providersByActive("false") : []);
//    var order = adProviderList.map(function(index, el) {
//        return({
//            id: $(this).attr("data-id"),
//            newRecord: $(this).attr("data-new-record"),
//            active: $(this).attr("data-active"),
//            waterfallOrder: index.toString(),
//            cpm: $(this).attr("data-cpm"),
//            configurable: $(this).attr("data-configurable"),
//            pending: $(this).attr("data-pending")
//        });
//    }).get();
//    return(JSON.stringify({adProviderOrder: order, optimizedOrder: optimizedOrder, testMode: testMode, appToken: appToken, generationNumber: generationNumber}));
//};
//
//$("#waterfall-list").sortable({containment: ".content"});
//$("#edit-waterfall-ad-provider").dialog({modal: true, autoOpen: false, minHeight: 500});
//
//if(optimizeToggleButton.prop("checked")) {
//    // Order ad providers by eCPM, disable sortable list, and hide draggable icon if waterfall has optimized_order set to true.
//    orderList("data-cpm", false);
//    $("#waterfall-list").sortable("disable");
//    $(".waterfall-drag").hide();
//} else {
//    // Enable sortable list if waterfall has optimized_order set to true.
//    $("#waterfall-list").sortable("enable");
//}
//
//// Adds event listeners to appropriate inputs when the WaterfallAdProvider edit modal pops up.
//var setRefreshOnRestartListeners = function() {
//    $(":input[data-refresh-on-app-restart=true]").change(function(event) {
//        var param = $(event.target.parentElement).children("label").html();
//        appRestartParams[param] = true;
//    });
//};
//
//$(document).ready(function() {
//    // Initiates AJAX request to update waterfall.
//    $(":button[name=submit]").click(function() {
//        postWaterfallUpdate();
//    });
//
//    // Direct the user to the selected Waterfall edit page.
//    waterfallSelection.change(function() {
//        window.location.href = waterfallSelection.val();
//    });
//
//    // Controls activation/deactivation of each ad provider in a waterfall.
//    $("button[name=status]").click(function(event) {
//        var listItem = $(event.target).parents("li");
//        var originalVal = listItem.attr("data-active") === "true";
//        listItem.children(".hideable").toggleClass("hidden-wap-info", originalVal);
//        listItem.attr("data-active", (!originalVal).toString());
//        listItem.toggleClass("inactive");
//        if(listItem.attr("data-new-record") === "true") {
//            createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: true});
//        } else {
//            postWaterfallUpdate();
//        }
//    });
//
//    // Opens modal for editing waterfall ad provider configuration info.
//    $(":button[name=configure-wap]").click(function(event) {
//        var listItem = $(event.target).parents("li");
//        var waterfallAdProviderID = listItem.attr("data-id");
//        if(listItem.attr("data-new-record") === "true") {
//            createWaterfallAdProvider({adProviderID: listItem.attr("data-id"), cpm: listItem.attr("data-cpm"), configurable: listItem.attr("data-configurable"), active: false}, true);
//        } else {
//            retrieveConfigData(waterfallAdProviderID);
//        }
//    });
//
//    // Click event for when Optimized Mode is toggled.
//    optimizeToggleButton.click(function() {
//        $(".waterfall-drag").toggle();
//        var sortableOption;
//        if(optimizeToggleButton.prop("checked")) {
//            sortableOption = "disable";
//            orderList("data-cpm", false);
//        } else {
//            sortableOption = "enable";
//        }
//        $("#waterfall-list").sortable(sortableOption);
//        postWaterfallUpdate();
//    });
//
//    // Click event for when Test Mode is toggled.
//    testModeButton.click(function(event) {
//        var waterfallListItems = $("#waterfall-list").children("li");
//        var newRecords = waterfallListItems.filter(function(index, li) { return($(li).attr("data-new-record") === "true") }).length;
//        var allRecords = waterfallListItems.length;
//        // Prevent the user from setting the waterfall to live without configuring any ad providers.
//        if(newRecords == allRecords) {
//            event.preventDefault();
//            flashMessage("You must activate an ad provider before the waterfall can go live.", waterfallErrorDiv);
//        } else {
//            postWaterfallUpdate();
//        }
//    });
//
//    // Sends AJAX request when waterfall order is changed via drag and drop.
//    $("#waterfall-list").on("sortdeactivate", function(event, ui) {
//        postWaterfallUpdate();
//    });
//});
//
