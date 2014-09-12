/**
 * Analytics Dashboard JS
 *
 * Uses the Keen.IO library to build the analytics dashboard.
 *
 * Creates a datepicker to be used for date filtering.  Binds country and adprovider dropdown for data filtering.
 */
// Include the Keen.IO library
!function(a,b){if(void 0===b[a]){b["_"+a]={},b[a]=function(c){b["_"+a].clients=b["_"+a].clients||{},b["_"+a].clients[c.projectId]=this,this._config=c},b[a].ready=function(c){b["_"+a].ready=b["_"+a].ready||[],b["_"+a].ready.push(c)};for(var c=["addEvent","setGlobalProperties","trackExternalLink","on"],d=0;d<c.length;d++){var e=c[d],f=function(a){return function(){return this["_"+a]=this["_"+a]||[],this["_"+a].push(arguments),this}};b[a].prototype[e]=f(e)}var g=document.createElement("script");g.type="text/javascript",g.async=!0,g.src="https://d26b395fwzu5fz.cloudfront.net/3.0.7/keen.min.js";var h=document.getElementsByTagName("script")[0];h.parentNode.insertBefore(g,h)}}("Keen",this);

// Initializes the keen library
var client = new Keen({
    projectId: $("#keen_project").val(),
    readKey: $("#scoped_key").val()
});

/**
 * Check if date is valid.  Provide a valid Javascript date object.
 * @param date
 * @returns {boolean}
 */
function isValidDate( date ) {
    if ( isNaN( date.getTime() ) ) {
        return false;
    }
    return true;
}

/**
 * Update charts on dashboard page.  Uses the currently set dropdowns and dates.  This can be called anytime we want
 * to update the dashboard.
 */
function updateCharts() {
    // Get current dropdown values
    var country = $( '#countries' ).val();
    var adProvider = $( '#ad_providers' ).val();
    var appID = $( '#app_id' ).val();
    var eCPM = $( '#ecpm' ).val();

    var start_date = $( '#start_date' ).datepicker( 'getUTCDate' );
    var end_date = $( '#end_date' ).datepicker( 'getUTCDate' );
    // Return if one or both of the dates are invalid
    if ( !isValidDate( start_date ) || !isValidDate( end_date ) ) {
        return;
    }

    // Return if start date after end date
    if ( end_date.getTime() <= start_date.getTime() ) {
        return;
    }

    // Build filters based on the dropdown selections and app_id
    var filters = [];
    filters.push( {
        property_name: "app_id",
        operator: "eq",
        property_value: appID
    } );
    // ip_geo_info.country is generated using the IP address by keen.
    if ( country !== "all" ) {
        filters.push( {
            property_name: "ip_geo_info.country",
            operator: "eq",
            property_value: country
        } );
    }
    if ( adProvider !== "all" ) {
        filters.push( {
            property_name: "ad_provider",
            operator: "eq",
            property_value: adProvider
        } );
    }
    var start_date_iso = start_date.toISOString();
    var end_date_iso = end_date.toISOString();

    // Only create the charts if keen is ready
    Keen.ready( function() {
        // Inventory Request count, metric
        var inventory_request = new Keen.Query( "count", {
            eventCollection: "inventory_request",
            filters: filters,
            timeframe: {
                start: start_date_iso,
                end: end_date_iso
            }
        } );
        client.draw( inventory_request, document.getElementById( "inventory_requests" ), {
            chartType: "metric",
            title: "Requests",
            colors: [ "#4285f4" ],
            width: $( "#inventory_requests" ).width()
        });

        // Calculate fill rate using inventory requests divided by inventory_available
        var available_count = new Keen.Query( "count", {
            eventCollection: "inventory_available",
            filters: filters,
            timeframe: {
                start: start_date_iso,
                end: end_date_iso
            }
        } );

        client.run( [ inventory_request, available_count ], function() {
            var conversion_rate = 0;
            if ( this.data[0].result !== 0 ) {
                conversion_rate = ( this.data[1].result / this.data[0].result ).toFixed(2 )*100
            }

            new Keen.Visualization( { result: conversion_rate }, document.getElementById( 'fill_rate' ), {
                chartType: "metric",
                title: "Fill Rate",
                chartOptions: {
                    suffix: "%"
                },
                colors: [ "#4285f4" ],
                width: $( "#fill_rate" ).width()
            } );
        } );

        // Estimated Revenue query
        var estimated_revenue = new Keen.Query( "count", {
            eventCollection: "ad_completed",
            interval: "daily",
            filters: filters,
            timeframe: {
                start: start_date_iso,
                end: end_date_iso
            }
        } );

        // Calculate expected eCPM
        client.run( estimated_revenue, function() {
            var table_data = [];
            var chart_data = [];
            var cumulative_revenue = 0;
            _.each( this.data.result, function ( day ) {
                var days_revenue = (day.value * eCPM);
                var date = new Date( day.timeframe.start );
                var date_string = ( date.getUTCMonth() + 1 ) + "/" + date.getUTCDate() + "/" + date.getUTCFullYear();
                table_data.push( {
                    "Date": date_string,
                    "Estimated Revenue": '$' + days_revenue
                } );
                chart_data.push( {
                    "Date": date_string,
                    "Estimated Revenue": days_revenue
                } );
                cumulative_revenue = cumulative_revenue + days_revenue;
            } );

            var average_revenue = {
                result: cumulative_revenue / this.data.result.length
            };
            new Keen.Visualization( average_revenue, document.getElementById( "unique_users" ), {
                chartType: "metric",
                title: "Average Revenue By Day",
                colors: [ "#4285f4" ],
                    chartOptions: {
                    prefix: "$"
                },
                width: $( "#unique_users" ).width()
            });

            // Estimated Revenue Table
            new Keen.Visualization( { result: table_data.reverse() }, document.getElementById( "estimated_revenue" ), {
                chartType: "table",
                title: "Estimated Revenue",
                colors: [ "#4285f4" ],
                width: $( "#estimated_revenue" ).width()
            } );

            // Estimated Revenue Chart
            new Keen.Visualization( { result: chart_data }, document.getElementById("estimated_revenue_chart"), {
                chartType: "areachart",
                title: false,
                height: 250,
                width: "auto",
                filters: filters,
                chartOptions: {
                    chartArea: {
                        height: "85%",
                        left: "5%",
                        top: "5%",
                        width: "80%"
                    },
                    isStacked: true
                }
            } );
        } );
    });
};

// Create date range picker
$( '.input-daterange' ).datepicker( {
    orientation: "top left"
} ).on( "changeDate", updateCharts );

// Set initial start date to the last 30days
$( '#start_date' ).datepicker( 'setDate', '-1m');
$( '#end_date' ).datepicker( 'setDate', '0');

// Bind update events on dropdown changes
$( '#countries, #ad_providers' ).change( updateCharts );