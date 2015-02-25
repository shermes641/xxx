/**
 * Analytics Dashboard JS
 *
 * Uses the Keen.IO library to build the analytics dashboard.
 *
 * Creates a datepicker to be used for date filtering.  Binds country and adprovider dropdown for data filtering.
 */
mediationModule.controller( 'AnalyticsController', [ '$scope', '$http', '$routeParams', 'appCheck', '$filter', '$timeout', '$document',
    function( $scope, $http, $routeParams, appCheck, $filter, $timeout, $document ) {
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
                    available: data.adProviders,
                    input: ""
                },
                apps: {
                    open: false,
                    selected: [{id: 'all', name: 'All Apps'}],
                    available: data.apps,
                    input: ""
                },
                countries: {
                    open: false,
                    selected: [{id: 'all', name: 'All Countries'}],
                    available: [{id:'United States', name:'United States'}, {id:'Afghanistan', name:'Afghanistan'},{id:'Albania', name:'Albania'},{id:'Algeria', name:'Algeria'},{id:'Andorra', name:'Andorra'},{id:'Angola', name:'Angola'},{id:'Antarctica', name:'Antarctica'},{id:'Antigua and Barbuda', name:'Antigua and Barbuda'},{id:'Argentina', name:'Argentina'},{id:'Armenia', name:'Armenia'},{id:'Australia', name:'Australia'},{id:'Austria', name:'Austria'},{id:'Azerbaijan', name:'Azerbaijan'},{id:'Bahamas', name:'Bahamas'},{id:'Bahrain', name:'Bahrain'},{id:'Bangladesh', name:'Bangladesh'},{id:'Barbados', name:'Barbados'},{id:'Belarus', name:'Belarus'},{id:'Belgium', name:'Belgium'},{id:'Belize', name:'Belize'},{id:'Benin', name:'Benin'},{id:'Bermuda', name:'Bermuda'},{id:'Bhutan', name:'Bhutan'},{id:'Bolivia', name:'Bolivia'},{id:'Bosnia and Herzegovina', name:'Bosnia and Herzegovina'},{id:'Botswana', name:'Botswana'},{id:'Brazil', name:'Brazil'},{id:'Brunei', name:'Brunei'},{id:'Bulgaria', name:'Bulgaria'},{id:'Burkina Faso', name:'Burkina Faso'},{id:'Burma', name:'Burma'},{id:'Burundi', name:'Burundi'},{id:'Cambodia', name:'Cambodia'},{id:'Cameroon', name:'Cameroon'},{id:'Canada', name:'Canada'},{id:'Cape Verde', name:'Cape Verde'},{id:'Central African Republic', name:'Central African Republic'},{id:'Chad', name:'Chad'},{id:'Chile', name:'Chile'},{id:'China', name:'China'},{id:'Colombia', name:'Colombia'},{id:'Comoros', name:'Comoros'},{id:'Congo, Democratic Republic', name:'Congo, Democratic Republic'},{id:'Congo, Republic of the', name:'Congo, Republic of the'},{id:'Costa Rica', name:'Costa Rica'},{id:'Cote d Ivoire', name:'Cote d Ivoire'},{id:'Croatia', name:'Croatia'},{id:'Cuba', name:'Cuba'},{id:'Cyprus', name:'Cyprus'},{id:'Czech Republic', name:'Czech Republic'},{id:'Denmark', name:'Denmark'},{id:'Djibouti', name:'Djibouti'},{id:'Dominica', name:'Dominica'},{id:'Dominican Republic', name:'Dominican Republic'},{id:'East Timor', name:'East Timor'},{id:'Ecuador', name:'Ecuador'},{id:'Egypt', name:'Egypt'},{id:'El Salvador', name:'El Salvador'},{id:'Equatorial Guinea', name:'Equatorial Guinea'},{id:'Eritrea', name:'Eritrea'},{id:'Estonia', name:'Estonia'},{id:'Ethiopia', name:'Ethiopia'},{id:'Fiji', name:'Fiji'},{id:'Finland', name:'Finland'},{id:'France', name:'France'},{id:'Gabon', name:'Gabon'},{id:'Gambia', name:'Gambia'},{id:'Georgia', name:'Georgia'},{id:'Germany', name:'Germany'},{id:'Ghana', name:'Ghana'},{id:'Greece', name:'Greece'},{id:'Greenland', name:'Greenland'},{id:'Grenada', name:'Grenada'},{id:'Guatemala', name:'Guatemala'},{id:'Guinea', name:'Guinea'},{id:'Guinea-Bissau', name:'Guinea-Bissau'},{id:'Guyana', name:'Guyana'},{id:'Haiti', name:'Haiti'},{id:'Honduras', name:'Honduras'},{id:'Hong Kong', name:'Hong Kong'},{id:'Hungary', name:'Hungary'},{id:'Iceland', name:'Iceland'},{id:'India', name:'India'},{id:'Indonesia', name:'Indonesia'},{id:'Iran', name:'Iran'},{id:'Iraq', name:'Iraq'},{id:'Ireland', name:'Ireland'},{id:'Israel', name:'Israel'},{id:'Italy', name:'Italy'},{id:'Jamaica', name:'Jamaica'},{id:'Japan', name:'Japan'},{id:'Jordan', name:'Jordan'},{id:'Kazakhstan', name:'Kazakhstan'},{id:'Kenya', name:'Kenya'},{id:'Kiribati', name:'Kiribati'},{id:'Korea, North', name:'Korea, North'},{id:'Korea, South', name:'Korea, South'},{id:'Kuwait', name:'Kuwait'},{id:'Kyrgyzstan', name:'Kyrgyzstan'},{id:'Laos', name:'Laos'},{id:'Latvia', name:'Latvia'},{id:'Lebanon', name:'Lebanon'},{id:'Lesotho', name:'Lesotho'},{id:'Liberia', name:'Liberia'},{id:'Libya', name:'Libya'},{id:'Liechtenstein', name:'Liechtenstein'},{id:'Lithuania', name:'Lithuania'},{id:'Luxembourg', name:'Luxembourg'},{id:'Macedonia', name:'Macedonia'},{id:'Madagascar', name:'Madagascar'},{id:'Malawi', name:'Malawi'},{id:'Malaysia', name:'Malaysia'},{id:'Maldives', name:'Maldives'},{id:'Mali', name:'Mali'},{id:'Malta', name:'Malta'},{id:'Marshall Islands', name:'Marshall Islands'},{id:'Mauritania', name:'Mauritania'},{id:'Mauritius', name:'Mauritius'},{id:'Mexico', name:'Mexico'},{id:'Micronesia', name:'Micronesia'},{id:'Moldova', name:'Moldova'},{id:'Mongolia', name:'Mongolia'},{id:'Morocco', name:'Morocco'},{id:'Monaco', name:'Monaco'},{id:'Mozambique', name:'Mozambique'},{id:'Namibia', name:'Namibia'},{id:'Nauru', name:'Nauru'},{id:'Nepal', name:'Nepal'},{id:'Netherlands', name:'Netherlands'},{id:'New Zealand', name:'New Zealand'},{id:'Nicaragua', name:'Nicaragua'},{id:'Niger', name:'Niger'},{id:'Nigeria', name:'Nigeria'},{id:'Norway', name:'Norway'},{id:'Oman', name:'Oman'},{id:'Pakistan', name:'Pakistan'},{id:'Panama', name:'Panama'},{id:'Papua New Guinea', name:'Papua New Guinea'},{id:'Paraguay', name:'Paraguay'},{id:'Peru', name:'Peru'},{id:'Philippines', name:'Philippines'},{id:'Poland', name:'Poland'},{id:'Portugal', name:'Portugal'},{id:'Qatar', name:'Qatar'},{id:'Romania', name:'Romania'},{id:'Russia', name:'Russia'},{id:'Rwanda', name:'Rwanda'},{id:'Samoa', name:'Samoa'},{id:'San Marino', name:'San Marino'},{id:'Sao Tome', name:'Sao Tome'},{id:'Saudi Arabia', name:'Saudi Arabia'},{id:'Senegal', name:'Senegal'},{id:'Serbia and Montenegro', name:'Serbia and Montenegro'},{id:'Seychelles', name:'Seychelles'},{id:'Sierra Leone', name:'Sierra Leone'},{id:'Singapore', name:'Singapore'},{id:'Slovakia', name:'Slovakia'},{id:'Slovenia', name:'Slovenia'},{id:'Solomon Islands', name:'Solomon Islands'},{id:'Somalia', name:'Somalia'},{id:'South Africa', name:'South Africa'},{id:'Spain', name:'Spain'},{id:'Sri Lanka', name:'Sri Lanka'},{id:'Sudan', name:'Sudan'},{id:'Suriname', name:'Suriname'},{id:'Swaziland', name:'Swaziland'},{id:'Sweden', name:'Sweden'},{id:'Switzerland', name:'Switzerland'},{id:'Syria', name:'Syria'},{id:'Taiwan', name:'Taiwan'},{id:'Tajikistan', name:'Tajikistan'},{id:'Tanzania', name:'Tanzania'},{id:'Thailand', name:'Thailand'},{id:'Togo', name:'Togo'},{id:'Tonga', name:'Tonga'},{id:'Trinidad and Tobago', name:'Trinidad and Tobago'},{id:'Tunisia', name:'Tunisia'},{id:'Turkey', name:'Turkey'},{id:'Turkmenistan', name:'Turkmenistan'},{id:'Uganda', name:'Uganda'},{id:'Ukraine', name:'Ukraine'},{id:'United Arab Emirates', name:'United Arab Emirates'},{id:'United Kingdom', name:'United Kingdom'},{id:'Uruguay', name:'Uruguay'},{id:'Uzbekistan', name:'Uzbekistan'},{id:'Vanuatu', name:'Vanuatu'},{id:'Venezuela', name:'Venezuela'},{id:'Vietnam', name:'Vietnam'},{id:'Yemen', name:'Yemen'},{id:'Zambia', name:'Zambia'},{id:'Zimbabwe', name:'Zimbabwe'}],
                    input: ""
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

        $scope.resetAllFilters = function() {
            _.each($scope.filters, function(filter) {
                filter.selected = [{id:'all', name:'all'}];
            });
        };

        // Close filter dropdown
        $scope.closeDropdown = function(filterType) {
            // Only close dropdown if we are sure the user is done with the input
            $timeout(function() {
                if(document.activeElement.tagName === "BODY") {
                    $scope.filters[filterType].input = "";
                    $scope.filters[filterType].open = false;
                }
            }, 200);
        };

        // Open drop down for a given filter type
        $scope.openDropdown = function(filterType) {
            $scope.filters[filterType].input = "";
            if($scope.filters[filterType].open !== true){
                $scope.filters[filterType].open = true;
                $timeout(function() {
                    document.getElementById('filter_' + filterType).focus();
                });
            }
        };

        // Update Dropdown for a given filter
        $scope.addToSelected = function(filterType, object) {
            $scope.filters[filterType].input = "";
            if(object.id !== 'all'){
                // Remove "all" if another item is selected
                var all = _.find($scope.filters[filterType].selected, function(item){ return item.id === 'all'; });
                if(typeof all !== "undefined") {
                    $scope.filters[filterType].selected = _.reject($scope.filters[filterType].selected, function(object){ return object.id === 'all'; });
                    $scope.filters[filterType].available.push(all);
                }
            } else {
                // Remove selected items if "all" is selected
                $scope.filters[filterType].available = $scope.filters[filterType].available.concat($scope.filters[filterType].selected);
                $scope.filters[filterType].selected = [];
            }
            $scope.filters[filterType].available = _.reject($scope.filters[filterType].available, function(item){ return item.id === object.id; });
            $scope.filters[filterType].selected.push(object);
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