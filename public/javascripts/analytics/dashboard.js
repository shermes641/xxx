/**
 * Analytics Dashboard JS
 *
 * Uses the Keen.IO library to build the analytics dashboard.
 *
 * Creates a datepicker to be used for date filtering.  Binds country and adprovider dropdown for data filtering.
 */
// Initializes the keen library
var client = new Keen( {
    projectId: $( "#keen_project" ).val(),
    readKey: $( "#scoped_key" ).val()
} );

var Analytics = function () {
    this.elements = {
        country: $( '#countries' ),
        adProvider: $( '#ad_providers' ),
        apps: $( '#apps' ),
        startDate: $( '#start_date' ),
        endDate: $( '#end_date' )
    };

    /**
     * Check if date is valid.  Provide a valid Javascript date object.
     * @param date
     * @returns {boolean}
     */
    this.isValidDate = function( date ) {
        if ( isNaN( date.getTime() ) ) {
            return false;
        }
        return true;
    };

    /**
     * Returns filters to use for charting and graphs
     * @param apps An array of apps to include
     * @param country An array of countries to include
     * @param adProvider An array of ad Providers to include
     * @returns filter Array to be used in keen queries
     */
    this.buildFilters = function( apps, country, adProvider ) {
        var filters = [];
        if ( apps.indexOf( "all" ) === -1 && apps.length !== 0 ) {
            filters.push( {
                property_name: "app_id",
                operator: "in",
                property_value: apps
            } );
        }
        // ip_geo_info.country is generated using the IP address by keen.
        if ( country.indexOf( "all" ) === -1 && country.length !== 0 ) {
            filters.push( {
                property_name: "ip_geo_info.country",
                operator: "in",
                property_value: country
            } );
        }
        if ( adProvider.indexOf( "all" ) === -1 && adProvider.length !== 0 ) {
            filters.push( {
                property_name: "ad_provider_id",
                operator: "in",
                property_value: adProvider
            } );
        }

        return filters
    };

    /**
     * Update charts on dashboard page.  Uses the currently set dropdowns and dates.  This can be called anytime we want
     * to update the dashboard.
     */
    this.updateCharts = function() {
        // Get current element values
        // We do this every update just incase any fields have changed (including hidden)
        var country = this.selectize.country.getValue(),
            adProvider = this.selectize.adProvider.getValue(),
            apps = this.selectize.apps.getValue(),
            start_date = this.elements.startDate.datepicker( 'getUTCDate'),
            end_date = this.elements.endDate.datepicker( 'getUTCDate' );

        // Return if one or both of the dates are invalid
        if ( !this.isValidDate( start_date ) || !this.isValidDate( end_date ) ) {
            return;
        }

        // Return if start date after end date
        if ( end_date.getTime() <= start_date.getTime() ) {
            return;
        }

        // Build filters based on the dropdown selections and app_id
        var filters = this.buildFilters( apps, country, adProvider );

        var start_date_iso = start_date.toISOString();
        end_date.setHours(end_date.getHours() + 40);
        var end_date_iso = end_date.toISOString();

        // Empty styled metric
        var empty_metric = function ( element_id, title ) {
            var element = $( "#" + element_id );
            var template = '<div class="keen-widget keen-metric" style="background-color: #aaaaaa; width:' + element.width() + 'px;"><span class="keen-metric-value"><span class="keen-metric-suffix">N/A</span></span><span class="keen-metric-title">' + title + '</span></div>';
            element.html( template );
        };

        // Only create the charts if keen is ready
        Keen.ready( function() {

            // Ad Provider eCPM
            var ecpm_metric = new Keen.Query( "average", {
                eventCollection: "ad_completed",
                targetProperty: "ad_provider_eCPM",
                filters: filters,
                timeframe: {
                    start: start_date_iso,
                    end: end_date_iso
                }
            } );

            client.run( ecpm_metric, function() {
                var eCPM = 0;
                if ( this.data.result === null ) {
                    empty_metric( "ecpm_metric", "eCPM" );
                } else {
                    new Keen.Visualization( this.data, document.getElementById( "ecpm_metric" ), {
                        chartType: "metric",
                        title: "eCPM",
                        chartOptions: {
                            decimals: 2
                        },
                        colors: [ "#4285f4" ],
                        width: $( "#ecpm_metric" ).width()
                    });

                    eCPM = this.data.result;
                }

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
                            prefix: "$",
                            decimals: 2
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
            } );

            if ( adProvider.length > 1 ) {
                empty_metric( "fill_rate", "Fill Rate" );
            } else {
                var request_collection = "availability_requested";
                var response_collection = "availability_response_true";

                // If all or no ad providers are selected show waterfall fill rate
                if ( adProvider.indexOf( "all" ) !== -1 ) {
                    request_collection = "mediation_availability_requested";
                    response_collection = "mediation_availability_response_true";
                }

                // Inventory Request count, metric
                var inventory_request = new Keen.Query( "count", {
                    eventCollection: request_collection,
                    filters: filters,
                    timeframe: {
                        start: start_date_iso,
                        end: end_date_iso
                    }
                } );

                // Calculate fill rate using inventory requests divided by inventory_available
                var available_count = new Keen.Query( "count", {
                    eventCollection: response_collection,
                    filters: filters,
                    timeframe: {
                        start: start_date_iso,
                        end: end_date_iso
                    }
                } );

                client.run( [ inventory_request, available_count ], function() {
                    var conversion_rate = 0;
                    if ( this.data[ 0 ].result !== 0 ) {
                        conversion_rate = ( this.data[ 1 ].result / this.data[ 0 ].result ).toFixed( 2 )*100
                    }

                    // All or no ad providers are selected show waterfall fill rate.  If a single ad provider is selected show
                    // that ad providers fill rate.  If multiple ad providers are selected show n/a.
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
            }
        });
    };

    var selectizeOptions = {
        maxItems: 6,
        plugins: ['remove_button'],
        onChange: _.bind( this.updateCharts, this ),
        onItemAdd: function( value ) {
            if( value !== "all" ) {
                this.removeItem( "all" );
                this.refreshItems();
            }
        }
    };

    this.getSelectizeInstance = function( element ) {
        if( typeof element.selectize( selectizeOptions )[0] !== 'undefined' ){
            return element.selectize( selectizeOptions )[0].selectize;
        } else {
            return false;
        }
    }
    this.selectize = {
        country: this.getSelectizeInstance( this.elements.country ),
        apps: this.getSelectizeInstance( this.elements.apps ),
        adProvider: this.getSelectizeInstance( this.elements.adProvider )
    };

    // Create date range picker
    $( '.input-daterange' ).datepicker( {
        orientation: "top left"
    } ).on( "changeDate", _.bind( this.updateCharts, this ) );

    // Set initial start date to the last 30days
    this.elements.startDate.datepicker( 'setDate', '-1m');
    this.elements.endDate.datepicker( 'setDate', '0');
}