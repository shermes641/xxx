/**
 * Analytics Dashboard JS
 *
 * Uses the Keen.IO library to build the analytics dashboard.
 *
 * Creates a datepicker to be used for date filtering.  Binds country and adprovider dropdown for data filtering.
 */
mediationModule.controller( 'AnalyticsController', [ '$scope', '$http', '$routeParams', 'appCheck', '$filter', '$timeout',
    function( $scope, $http, $routeParams, appCheck, $filter, $timeout ) {
        $scope.subHeader = 'assets/templates/sub_header.html';
        $scope.page = 'analytics';

        // Retrieve Waterfall data
        $http.get('/distributors/' + $routeParams.distributorID + '/analytics/info').success(function(data) {
            $scope.distributorID = $routeParams.distributorID;
            $scope.adProviders = data.adProviders;
            $scope.apps = data.apps;

            $scope.filters = {
                ad_providers: {
                    open: false,
                    selected: [{id: 'all', name: 'All Ad Providers'}],
                    available: data.adProviders
                },
                apps: {
                    open: false,
                    selected: [{id: 'all', name: 'All Apps'}],
                    available: data.apps
                }
            };

            $scope.scopedKey = data.scopedKey;
            $scope.keenProject = data.keenProject;
            $scope.startDatepicker();

            // Initializes the keen library
            $scope.keenClient = new Keen( {
                projectId: $scope.keenProject,
                readKey: $scope.scopedKey
            } );

        });

        // Open drop down for a given filter type
        $scope.openDropdown = function(filterType) {
            if($scope.filters[filterType].open !== true){
                $scope.filters[filterType].available = _.sortBy($scope.filters[filterType].available, 'name');
                $scope.filters[filterType].open = true;
                $timeout(function() {
                    document.getElementById('filter_' + filterType).focus();
                });
            }
        };

        // Close filter dropdown
        $scope.closeDropdown = function(filterType) {
            $scope.filters[filterType].open = false;
            $scope.filters[filterType].selected = _.sortBy($scope.filters[filterType].selected, 'name');
        };

        // Update Dropdown for a given filter
        $scope.addToSelected = function(filterType, object, index) {
            if(object.id !== 'all'){
                // Remove "all" if another item is selected
                var all = _.find($scope.filters[filterType].selected, function(object){ return object.id === 'all'; });
                if(typeof all !== "undefined") {
                    $scope.filters[filterType].selected = _.reject($scope.filters[filterType].selected, function(object){ return object.id === 'all'; });
                    $scope.filters[filterType].available.push(all);
                }
            } else {
                // Remove selected items if "all" is selected
                $scope.filters[filterType].available = $scope.filters[filterType].available.concat($scope.filters[filterType].selected);
                $scope.filters[filterType].selected = [];
            }
            $scope.filters[filterType].selected.push(object);
            $scope.filters[filterType].available.splice(index, 1);
        };

        $scope.removeFromSelected = function(filterType, object, index) {
            if(object.id !== 'all'){
                $scope.filters[filterType].available.push(object);
                $scope.filters[filterType].selected.splice(index, 1);
            }
            // If nothing selected add all
            if($scope.filters[filterType].selected.length === 0) {
                var all = _.find($scope.filters[filterType].available, function(object){ return object.id === 'all'; });
                $scope.filters[filterType].available = _.reject($scope.filters[filterType].available, function(object){ return object.id === 'all'; });
                $scope.filters[filterType].selected.push(all);
            }
        };


        $scope.startDatepicker = function() {
            $scope.elements = {
                country: $( '#countries' ),
                adProvider: $( '#ad_providers' ),
                apps: $( '#apps' ),
                startDate: $( '#start_date' ),
                endDate: $( '#end_date' ),
                exportAsCsv: $( '#export_as_csv' ),
                emailModal: $( '#email_modal' ),
                emailForm: $( '#csv_email_form' ),
                emailInput: $( '#export_email' ),
                overlay: $( '#analytics_overlay' ),
                submitExport: $( '#export_submit' ),
                exportComplete: $( '#csv_requested' ),
                exportError: $( '#csv_export_error' ),
                closeButton: $( '.close_button' )
            };

            // Distributor ID to be used in AJAX calls.
            $scope.exportEndpoint = "/distributors/" + $scope.distributorID + "/analytics/export";

            // Create date range picker
            $( '.input-daterange' ).datepicker( {
                orientation: "auto top",
                format: "M dd, yyyy"
            } ).on( "changeDate", $scope.updateCharts );

            // Set initial start date to the last 30days
            $scope.elements.startDate.datepicker( 'setDate', '-1m');
            $scope.elements.endDate.datepicker( 'setDate', '0');
        };

        /**
         * Pop up dialog for user to enter email
         */
        $scope.showEmailForm = function () {
            $scope.elements.emailModal.show();
            $scope.elements.emailForm.show();
            $scope.showOverlay();
        };

        /**
         * Begin CSV export and let the user know the export has been requested
         */
        $scope.submit = function() {
            if($scope.exportForm.$valid) {
                $scope.elements.emailForm.hide();
                var emailAddress = $scope.elements.emailInput.val();
                $http.post( $scope.exportEndpoint, { email: emailAddress })
                    .success(_.bind( function() {
                        $scope.elements.exportComplete.show();
                    }, $scope ))
                    .error( _.bind( function() {
                        $scope.elements.exportError.show();
                    }, $scope )
                );
            }
        };

        /**
         * Show overlay
         */
        $scope.showOverlay = function () {
            $scope.elements.overlay.show();
        };

        /**
         * Show overlay and other modal elements
         */
        $scope.hideOverlay = function () {
            $scope.elements.emailInput.val( "" );
            $scope.elements.overlay.hide();
            $scope.elements.emailModal.hide();
            $scope.elements.exportComplete.hide();
            $scope.elements.exportError.hide();
        };

        /**
         * Check if date is valid.  Provide a valid Javascript date object.
         * @param date
         * @returns {boolean}
         */
        $scope.isValidDate = function( date ) {
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
        $scope.buildFilters = function( apps, country, adProvider ) {
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
        $scope.updateCharts = function() {
            // Get current element values
            // We do this every update just incase any fields have changed (including hidden)
            var country = ['all'],
                adProvider = ['all'],
                apps = ['all'],
                start_date = $scope.elements.startDate.datepicker( 'getUTCDate'),
                end_date = $scope.elements.endDate.datepicker( 'getUTCDate' );

            // Return if one or both of the dates are invalid
            if ( !$scope.isValidDate( start_date ) || !$scope.isValidDate( end_date ) ) {
                return;
            }

            // Return if start date after end date
            if ( end_date.getTime() <= start_date.getTime() ) {
                return;
            }

            // Build filters based on the dropdown selections and app_id
            var filters = $scope.buildFilters( apps, country, adProvider );

            var start_date_iso = start_date.toISOString();
            end_date.setHours(end_date.getHours() + 40);
            var end_date_iso = end_date.toISOString();

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

                $scope.keenClient.run( ecpm_metric, function() {
                    var eCPM = 0;
                    if ( this.data.result === null ) {
                        $scope.ecpmMetric = "N/A";
                    } else {
                        var ecpmSplit = $filter("monetaryFormat")(this.data.result).split(".")
                        $scope.ecpmMetric = '<sup>$</sup>' + ecpmSplit[0] + '<sup>.' + ecpmSplit[1] + '</sup>';

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
                    $scope.keenClient.run( estimated_revenue, function() {
                        var table_data = [];
                        var chart_data = [];
                        var cumulative_revenue = 0;
                        _.each( this.data.result, function ( day ) {
                            var days_revenue = (day.value * eCPM);
                            var date_string = moment( day.timeframe.start).format("MMM DD, YYYY");

                            table_data.push( {
                                "Date": date_string,
                                "Estimated Revenue": '$' + $filter("monetaryFormat")(days_revenue)
                            } );
                            chart_data.push( {
                                "Date": date_string,
                                "Estimated Revenue": Number($filter("monetaryFormat")(days_revenue))
                            } );
                            cumulative_revenue = cumulative_revenue + days_revenue;
                        } );

                        var average_revenue = {
                            result: cumulative_revenue / this.data.result.length
                        };

                        var revenueSplit = $filter("monetaryFormat")(average_revenue.result).split(".")
                        $scope.revenueByDayMetric = '<sup>$</sup>' + revenueSplit[0] + '<sup>.' + revenueSplit[1] + '</sup>';
                        $scope.revenueTable = table_data.reverse();
                        $scope.$apply();

                        // Estimated Revenue Chart
                        new Keen.Visualization( { result: chart_data }, document.getElementById("estimated_revenue_chart"), {
                            chartType: "areachart",
                            title: false,
                            height: 250,
                            width: "auto",
                            colors: ["#42c187"],
                            filters: filters,
                            chartOptions: {
                                chartArea: {
                                    height: "85%",
                                    left: "5%",
                                    top: "5%",
                                    width: "93%"
                                },
                                legend: {
                                    position: "none"
                                },
                                vAxis: {
                                    viewWindowMode: "explicit",
                                    viewWindow:{
                                        min: 0
                                    },
                                    format: "$#,###",
                                    gridlines: {
                                        color: "#f2f2f2",
                                        count: 5
                                    },
                                    textStyle: {
                                        color: '#999999'
                                    }
                                },
                                hAxis: {
                                    gridlines: {
                                        color: "#d2d2d2"
                                    },
                                    textStyle: {
                                        color: '#999999'
                                    }
                                },
                                isStacked: true
                            }
                        } );
                    } );
                } );

                if ( adProvider.length > 1 ) {
                    $scope.fillRateMetric = "N/A";
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

                    $scope.keenClient.run( [ inventory_request, available_count ], function() {
                        var conversion_rate = 0;
                        if ( this.data[ 0 ].result !== 0 ) {
                            conversion_rate = ( this.data[ 1 ].result / this.data[ 0 ].result ).toFixed( 2 )*100
                        }

                        $scope.fillRateMetric = conversion_rate + '%';
                    } );
                }
            });
        };
    } ]
);