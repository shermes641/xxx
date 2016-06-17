describe('AnalyticsController', function () {
    var scope, testCont, $window, $httpBackend, configRequestHandler, server;

    beforeEach(module('MediationModule'));

    describe('AnalyticsController', function () {
        var scope, testCont, $window, $httpBackend, configRequestHandler, server, filter;
        server = sinon.fakeServer.create();
        server.respondImmediately = true;
        sinon.format = function (object) {
            return JSON.stringify(object);
        };
        sinon.log = function (message) {
            console.log(message);
        };

        var urlRoot = "https://api.keen.io/3.0/projects/5512efa246f9a74b786bc7d1/queries/";
        var apiKey = "api_key=D8DD8FDF000323000448F";

        var startOfDay = moment().startOf('day');
        var endDate = startOfDay.clone().add(1, 'days');

        var timeframe = 'timeframe=%7B%22start%22%3A%22' + startOfDay.format('YYYY-MM-DD') + "T00%3A00%3A00%2B00%3A00%22%2C%22end%22%3A%22" +
            endDate.format('YYYY-MM-DD') + "T00%3A00%3A00%2B00%3A00%22%7D&timezone=UTC";

        var listenForKeen = function (filter) {
            server.respondWith("GET", urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({"result": 5.01123595505618})]);
            server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": 147,
                        "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": 173,
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": {
                            "averageeCPM": 4.63265306122449,
                            "completedCount": 49
                        }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": {"averageeCPM": 5.475, "completedCount": 40},
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": 400,
                        "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": 597,
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": 400,
                        "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": 597,
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=ad_displayed&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": 147,
                        "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": 203,
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                [200, {"Content-Type": "application/json"}, JSON.stringify({
                    "result": [{
                        "value": 147,
                        "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                    }, {
                        "value": 173,
                        "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                    }]
                })]);
            console.log(urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe);

        };
        listenForKeen("");
        listenForKeen("%7B%22property_name%22%3A%22ad_provider_id%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B1%2C2%5D%7D");

        describe('analytics controller and landing page', function () {
            beforeEach(inject(function ($rootScope, $controller, _$window_, _$httpBackend_) {
                server = sinon.fakeServer.create();
                server.respondImmediately = true;
                sinon.format = function (object) {
                    return JSON.stringify(object);
                };
                sinon.log = function (message) {
                    console.log(message);
                };
                var listenForKeen = function (filter) {
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": 4.63265306122449,
                                "completedCount": 49
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": null,
                                "completedCount": 0
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": 4.63265306122449,
                                    "completedCount": 49
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": 5.475, "completedCount": 40},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=ad_displayed&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 203,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "average?" + apiKey + "&event_collection=reward_delivered&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({"result": null})]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": null,
                                    "completedCount": 0
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": null, "completedCount": 0},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    console.log(urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe);

                };
                listenForKeen("");
                listenForKeen("%7B%22property_name%22%3A%22ad_provider_name%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B%22UnityAds%22%2C%22HyprMarketplace%22%5D%7D");
                // Set up the mock http service responses
                $httpBackend = _$httpBackend_;

                configRequestHandler = $httpBackend.when('GET', '/distributors/undefined/analytics/info')
                    .respond({
                        "distributorID": 620798327,
                        "adProviders": [{"name": "Unity Ads", "id": "UnityAds"}, {
                            "name": "HyprMarketplace",
                            "id": "HyprMarketplace"
                        }, {"name": "Vungle", "id": "Vungle"}, {"name": "AppLovin", "id": "AppLovin"}],
                        "apps": [{"id": "578021165", "distributorID": 620798327, "name": "Zombie Game"}],
                        "keenProject": "5512efa246f9a74b786bc7d1",
                        "scopedKey": "D8DD8FDF000323000448F"
                    });

                scope = $rootScope.$new();
                $window = _$window_;
                scope.debounceWait = 0;
                angular.element(document.body).append('<input id="start-date" />');
                testCont = $controller('AnalyticsController', {$scope: scope});
                $httpBackend.flush();
            }));

            beforeEach(function (done) {
                setInterval(function () {
                    if (scope.currentlyUpdating === false) {
                        done();
                    }
                }, 100);
            });

            afterEach(function () {
                $httpBackend.verifyNoOutstandingExpectation();
                $httpBackend.verifyNoOutstandingRequest();
                server.restore();
                angular.element('#start-date').remove();
            });

            it('should be defined', function () {
                expect(testCont).toBeDefined();
            });

            it('should get config correctly', function () {
                expect(scope.scopedKey).toEqual("D8DD8FDF000323000448F");
                expect(scope.keenProject).toEqual("5512efa246f9a74b786bc7d1");

                expect(scope.filters.ad_providers.available.length).toEqual(4);
                expect(scope.filters.apps.available[0].name).toEqual("Zombie Game");
                expect(scope.filters.countries.available.length).toEqual(191);
            });

            it('should have the fillRateMetric correct', function () {
                expect(scope.analyticsData.fillRateMetric).toEqual("32%");
            });

            it('should be initalized correctly', function () {
                expect(scope.subHeader).toEqual('assets/templates/sub_header.html');
                expect(scope.page).toEqual('analytics');
                expect(scope.currentlyUpdating).toEqual(false);
                expect(scope.updatingStatus).toEqual("Updating...");
                expect(scope.keenTimeout).toEqual(45000);
            });

            it('should have the fill rate set to N/A when multiple ad providers are selected', function () {
                scope.addToSelected("ad_providers", {"name": "Unity Ads", "id": "UnityAds"});
                scope.addToSelected("ad_providers", {"name": "HyprMarketplace", "id": "HyprMarketplace"});
                scope.updateCharts();
                expect(scope.analyticsData.fillRateMetric).toEqual("N/A");
                expect(scope.analyticsData.revenueTable[0].fillRate).toEqual("N/A");
            });

            it('should build the export CSV filters correctly', function () {
                var filters;
                var unityAds = {"name": "Unity Ads", "id": "UnityAds"};
                var hyprMarketplace = {"name": "HyprMarketplace", "id": "HyprMarketplace"};
                var adProvidersFilterName = "ad_providers";

                scope.addToSelected(adProvidersFilterName, unityAds);
                scope.addToSelected(adProvidersFilterName, hyprMarketplace);
                scope.addToSelected("countries", {"name": "Ireland", "id": "Ireland"});

                var dates = scope.getStartEndDates();
                filters = scope.getExportCSVFilters(dates);
                expect(filters.apps[0]).toEqual("578021165");
                expect(filters.ad_providers_selected).toEqual(true);
                expect(filters.timeframe.start).toEqual(startOfDay.clone().format('YYYY-MM-DDT00:00:00+00:00'));
                expect(filters.timeframe.end).toEqual(startOfDay.clone().add(1, 'day').format('YYYY-MM-DDT00:00:00+00:00'));
                expect(filters.filters[0].property_name).toEqual("ip_geo_info.country");
                expect(filters.filters[0].property_value[0]).toEqual("Ireland");
                expect(filters.filters[1].property_name).toEqual("ad_provider_name");
                expect(filters.filters[1].property_value[0]).toEqual(unityAds.id);
                expect(filters.filters[1].property_value[1]).toEqual(hyprMarketplace.id);

                scope.removeFromSelected(adProvidersFilterName, unityAds, 0);
                scope.removeFromSelected(adProvidersFilterName, hyprMarketplace, 0);

                filters = scope.getExportCSVFilters(dates);
                expect(filters.apps[0]).toEqual("578021165");
                expect(filters.ad_providers_selected).toEqual(false);
                expect(filters.timeframe.start).toEqual(startOfDay.clone().format('YYYY-MM-DDT00:00:00+00:00'));
                expect(filters.timeframe.end).toEqual(startOfDay.clone().add(1, 'day').format('YYYY-MM-DDT00:00:00+00:00'));
                expect(filters.filters[0].property_name).toEqual("ip_geo_info.country");
                expect(filters.filters[0].property_value[0]).toEqual("Ireland");
            });

            it('should have moment.js available', function () {
                expect(moment).toBeDefined();
            });

            it('should parse the datepicker dates correctly if user is in EST/EDT', function () {
                // Should return Feb 16 to March 16
                var start = "Sun Feb 15 2015 19:00:00 GMT-0500 (EST)";
                var end = "Sun Mar 15 2015 20:00:00 GMT-0400 (EDT)";
                expect(moment(start).utc().format("YYYY-MM-DD")).toEqual("2015-02-16");
                expect(moment(end).utc().format("YYYY-MM-DD")).toEqual("2015-03-16");
                expect(moment(start).utc().format()).toEqual("2015-02-16T00:00:00+00:00");
                expect(moment(end).utc().add(1, 'days').format()).toEqual("2015-03-17T00:00:00+00:00");
            });

            it('should parse the datepicker dates and return the same result regardless of the users timezone', function () {
                // Should return Feb 26 to March 11
                start = "Wed Feb 25 2015 19:00:00 GMT-0500 (EST)";
                end = "Tue Mar 10 2015 20:00:00 GMT-0400 (EDT)";
                expect(moment(start).utc().format("YYYY-MM-DD")).toEqual("2015-02-26");
                expect(moment(end).utc().format("YYYY-MM-DD")).toEqual("2015-03-11");
                expect(moment(start).utc().format()).toEqual("2015-02-26T00:00:00+00:00");
                expect(moment(end).utc().add(1, 'days').format()).toEqual("2015-03-12T00:00:00+00:00");

                // Should return Feb 26 to March 11
                start = "Wed Feb 25 2015 16:00:00 GMT-0800 (PST)";
                end = "Tue Mar 10 2015 17:00:00 GMT-0700 (PDT)";
                expect(moment(start).utc().format("YYYY-MM-DD")).toEqual("2015-02-26");
                expect(moment(end).utc().format("YYYY-MM-DD")).toEqual("2015-03-11");
                expect(moment(start).utc().format()).toEqual("2015-02-26T00:00:00+00:00");
                expect(moment(end).utc().add(1, 'days').format()).toEqual("2015-03-12T00:00:00+00:00");

                // Should return Feb 26 to March 11
                start = "Thu Feb 26 2015 05:00:00 GMT+0500 (UZT)";
                end = "Wed Mar 11 2015 05:00:00 GMT+0500 (UZT)";
                expect(moment(start).utc().format("YYYY-MM-DD")).toEqual("2015-02-26");
                expect(moment(end).utc().format("YYYY-MM-DD")).toEqual("2015-03-11");
                expect(moment(start).utc().format()).toEqual("2015-02-26T00:00:00+00:00");
                expect(moment(end).utc().add(1, 'days').format()).toEqual("2015-03-12T00:00:00+00:00");
            });

            it('should trigger analytics update on resize', function () {
                scope.currentlyUpdating = false;
                $window.dispatchEvent(new Event('resize'));
                expect(scope.currentlyUpdating).toEqual(true);
            });

            it('should calculate revenue per day correctly', function () {
                expect(scope.calculateDayRevenue(1000, 1000, 1)).toEqual(1);
                expect(scope.calculateDayRevenue(500, 500, 1)).toEqual(0.5);
                expect(scope.calculateDayRevenue(2000, 2000, 1)).toEqual(2);
                expect(scope.calculateDayRevenue(1000, 2000, 0.5)).toEqual(1);
                expect(scope.calculateDayRevenue(10000, 12000, 20)).toEqual(240);
                expect(scope.calculateDayRevenue(100, 500, 50)).toEqual(25);
                expect(scope.calculateDayRevenue(10, 100, 50)).toEqual(5);
                expect(scope.calculateDayRevenue(0, 0, 10)).toEqual(0);
                expect(scope.calculateDayRevenue(10, 0, 10)).toEqual(0);
                expect(scope.calculateDayRevenue(0, 10, 10)).toEqual(0);
            });

            describe('weightedAverageEcpm', function () {
                it('should return 0 if there are no completions for ad_completed or reward_delivered', function () {
                    var completionCount = 0;
                    var averageEcpm = 5;
                    expect(scope.weightedAverageEcpm(averageEcpm, completionCount, averageEcpm, completionCount)).toEqual(0);
                });

                it('should return the average eCPM for ad_completed if there is no info for reward_delivered events', function () {
                    var adCompletedCount = 505;
                    var adCompletedAverageEcpm = 5.36234;
                    var rewardDeliveredCount = 0;
                    var rewardDeliveredAverageEcpm = null;
                    expect(
                        scope.weightedAverageEcpm(
                            adCompletedAverageEcpm,
                            adCompletedCount,
                            rewardDeliveredAverageEcpm,
                            rewardDeliveredCount
                        )
                    ).toEqual(adCompletedAverageEcpm);
                });

                it('should return the average eCPM for reward_delivered if there is no info for ad_completed events', function () {
                    var adCompletedCount = 0;
                    var adCompletedAverageEcpm = null;
                    var rewardDeliveredCount = 217;
                    var rewardDeliveredAverageEcpm = 20.2387;
                    expect(
                        scope.weightedAverageEcpm(
                            adCompletedAverageEcpm,
                            adCompletedCount,
                            rewardDeliveredAverageEcpm,
                            rewardDeliveredCount
                        )
                    ).toEqual(rewardDeliveredAverageEcpm);
                });

                it('should return the weighted average eCPM when there is info for both reward_delivered and ad_completed events', function () {
                    var adCompletedCount = 505;
                    var adCompletedAverageEcpm = 5.36234;
                    var rewardDeliveredCount = 217;
                    var rewardDeliveredAverageEcpm = 20.2387;
                    // Calculated using formula:
                    // (adCompletedCount * adCompletedAverageEcpm) + (rewardDeliveredCount * rewardDeliveredAverageEcpm) / (adCompletedCount + rewardDeliveredCount)
                    var expectedEcpm = 9.83348975069;
                    var calculatedEcpm = scope.weightedAverageEcpm(
                        adCompletedAverageEcpm,
                        adCompletedCount,
                        rewardDeliveredAverageEcpm,
                        rewardDeliveredCount
                    );
                    expect(parseFloat(calculatedEcpm).toFixed(2)).toEqual(parseFloat(expectedEcpm).toFixed(2));
                });
            });
        });

        describe('completion calculations using ad_completed collection', function () {
            beforeEach(inject(function ($rootScope, $controller, _$window_, _$httpBackend_, $filter) {
                server = sinon.fakeServer.create();
                server.respondImmediately = true;
                sinon.format = function (object) {
                    return JSON.stringify(object);
                };
                sinon.log = function (message) {
                    console.log(message);
                };

                var listenForKeen = function (filter) {
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": 4.63265306122449,
                                "completedCount": 49
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": null,
                                "completedCount": 0
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": 4.63265306122449,
                                    "completedCount": 49
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": 5.475, "completedCount": 40},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=ad_displayed&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 203,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "average?" + apiKey + "&event_collection=reward_delivered&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({"result": null})]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": null,
                                    "completedCount": 0
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": null, "completedCount": 0},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    console.log(urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe);
                };

                listenForKeen("");
                listenForKeen("%7B%22property_name%22%3A%22ad_provider_name%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B%22UnityAds%22%2C%22HyprMarketplace%22%5D%7D");


                // Set up the mock http service responses
                $httpBackend = _$httpBackend_;

                configRequestHandler = $httpBackend.when('GET', '/distributors/undefined/analytics/info')
                    .respond({
                        "distributorID": 620798327,
                        "adProviders": [{"name": "Unity Ads", "id": "UnityAds"}, {
                            "name": "HyprMarketplace",
                            "id": "HyprMarketplace"
                        }, {"name": "Vungle", "id": "Vungle"}, {"name": "AppLovin", "id": "AppLovin"}],
                        "apps": [{"id": "578021165", "distributorID": 620798327, "name": "Zombie Game"}],
                        "keenProject": "5512efa246f9a74b786bc7d1",
                        "scopedKey": "D8DD8FDF000323000448F"
                    });

                scope = $rootScope.$new();
                $window = _$window_;
                filter = $filter;
                scope.debounceWait = 0;
                angular.element(document.body).append('<input id="start-date" />');
                scope.defaultStartDate = new Date(moment.utc("2015-04-03T00:00:00.000Z").format());
                scope.defaultEndDate = new Date(moment.utc("2015-04-15T00:00:00.000Z").format());
                testCont = $controller('AnalyticsController', {$scope: scope});
                $httpBackend.flush();
            }));

            beforeEach(function (done) {
                setInterval(function () {
                    if (scope.currentlyUpdating === false) {
                        done();
                    }
                }, 100);
            });

            afterEach(function () {
                $httpBackend.verifyNoOutstandingExpectation();
                $httpBackend.verifyNoOutstandingRequest();
                server.restore();
                angular.element('#start-date').remove();
            });

            it('should have the ecpmMetric correct', function () {
                expect(scope.analyticsData.ecpmMetric).toEqual("<sup>$</sup>4<sup>.63</sup>");
            });

            it('should have the correct table data', function () {
                var completions = 40;
                var impressions = 203;
                var averageEcpm = 5.47;
                var estimatedRevenue = scope.calculateDayRevenue(completions, impressions, averageEcpm);
                expect(scope.analyticsData.revenueTable.length).toEqual(2);
                expect(scope.analyticsData.revenueTable[0].averageeCPM).toEqual("$" + averageEcpm.toString());
                expect(scope.analyticsData.revenueTable[0].date).toEqual("Apr 11, 2015");
                expect(scope.analyticsData.revenueTable[0].completedCount).toEqual(completions);
                expect(scope.analyticsData.revenueTable[0].estimatedRevenue).toEqual("$" + filter("monetaryFormat")(estimatedRevenue));
                expect(scope.analyticsData.revenueTable[0].fillRate).toEqual("29%");
                expect(scope.analyticsData.revenueTable[0].impressions).toEqual(impressions);
                expect(scope.analyticsData.revenueTable[0].requests).toEqual(597);
            });

            it('should have the revenueByDayMetric correct', function () {
                expect(scope.analyticsData.revenueByDayMetric).toEqual("<sup>$</sup>0<sup>.89</sup>");
            });
        });

        describe('completion calculations using reward_delivered collection', function () {
            beforeEach(inject(function ($rootScope, $controller, _$window_, _$httpBackend_, $filter) {
                server = sinon.fakeServer.create();
                server.respondImmediately = true;
                sinon.format = function (object) {
                    return JSON.stringify(object);
                };
                sinon.log = function (message) {
                    console.log(message);
                };
                var listenForKeen = function (filter) {
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": null,
                                "completedCount": 0
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": 9.544,
                                "completedCount": 37
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": null,
                                    "completedCount": 0
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": null, "completedCount": 0},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=ad_displayed&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 203,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "average?" + apiKey + "&event_collection=reward_delivered&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({"result": 8.55})]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": 12.55,
                                    "completedCount": 17
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": 6.99, "completedCount": 20},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    console.log(urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe);

                };
                listenForKeen("");
                listenForKeen("%7B%22property_name%22%3A%22ad_provider_name%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B%22UnityAds%22%2C%22HyprMarketplace%22%5D%7D");
                // Set up the mock http service responses
                $httpBackend = _$httpBackend_;

                configRequestHandler = $httpBackend.when('GET', '/distributors/undefined/analytics/info')
                    .respond({
                        "distributorID": 620798327,
                        "adProviders": [{"name": "Unity Ads", "id": "UnityAds"}, {
                            "name": "HyprMarketplace",
                            "id": "HyprMarketplace"
                        }, {"name": "Vungle", "id": "Vungle"}, {"name": "AppLovin", "id": "AppLovin"}],
                        "apps": [{"id": "578021165", "distributorID": 620798327, "name": "Zombie Game"}],
                        "keenProject": "5512efa246f9a74b786bc7d1",
                        "scopedKey": "D8DD8FDF000323000448F"
                    });

                scope = $rootScope.$new();
                filter = $filter;
                $window = _$window_;
                scope.debounceWait = 0;
                scope.defaultStartDate = new Date(moment.utc("2015-04-03T00:00:00.000Z").format());
                scope.defaultEndDate = new Date(moment.utc("2015-04-15T00:00:00.000Z").format());
                angular.element(document.body).append('<input id="start-date" />');
                testCont = $controller('AnalyticsController', {$scope: scope});
                $httpBackend.flush();
            }));

            beforeEach(function (done) {
                setInterval(function () {
                    if (scope.currentlyUpdating === false) {
                        done();
                    }
                }, 100);
            });

            afterEach(function () {
                $httpBackend.verifyNoOutstandingExpectation();
                $httpBackend.verifyNoOutstandingRequest();
                server.restore();
                angular.element('#start-date').remove();
            });

            it('should have the ecpmMetric correct', function () {
                expect(scope.analyticsData.ecpmMetric).toEqual("<sup>$</sup>9<sup>.54</sup>");
            });

            it('should have the correct table data', function () {
                var completions = 20;
                var impressions = 203;
                var averageEcpm = 6.99;
                var estimatedRevenue = scope.calculateDayRevenue(completions, impressions, averageEcpm);
                expect(scope.analyticsData.revenueTable.length).toEqual(2);
                expect(scope.analyticsData.revenueTable[0].averageeCPM).toEqual("$" + averageEcpm.toString());
                expect(scope.analyticsData.revenueTable[0].date).toEqual("Apr 11, 2015");
                expect(scope.analyticsData.revenueTable[0].completedCount).toEqual(completions);
                expect(scope.analyticsData.revenueTable[0].estimatedRevenue).toEqual("$" + filter("monetaryFormat")(estimatedRevenue));
                expect(scope.analyticsData.revenueTable[0].fillRate).toEqual("29%");
                expect(scope.analyticsData.revenueTable[0].impressions).toEqual(impressions);
                expect(scope.analyticsData.revenueTable[0].requests).toEqual(597);
            });

            it('should have the revenueByDayMetric correct', function () {
                expect(scope.analyticsData.revenueByDayMetric).toEqual("<sup>$</sup>1<sup>.63</sup>");
            });
        });

        describe('completion calculations using a combination of ad_completed and reward_delivered collections', function () {
            beforeEach(inject(function ($rootScope, $controller, _$window_, _$httpBackend_) {
                server = sinon.fakeServer.create();
                server.respondImmediately = true;
                sinon.format = function (object) {
                    return JSON.stringify(object);
                };
                sinon.log = function (message) {
                    console.log(message);
                };
                var listenForKeen = function (filter) {
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": 2.782,
                                "completedCount": 23
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": {
                                "averageeCPM": 9.544,
                                "completedCount": 37
                            }
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": 4.012,
                                    "completedCount": 5
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": 2.4377, "completedCount": 18},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=mediate_availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_requested&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 400,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 597,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=ad_displayed&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 203,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "count?" + apiKey + "&event_collection=availability_response_true&interval=daily&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": 147,
                                "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": 173,
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    server.respondWith("GET", urlRoot + "average?" + apiKey + "&event_collection=reward_delivered&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({"result": 8.55})]);
                    server.respondWith("GET", urlRoot + "multi_analysis?" + apiKey + "&event_collection=reward_delivered&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B" + filter + "%5D&" + timeframe,
                        [200, {"Content-Type": "application/json"}, JSON.stringify({
                            "result": [{
                                "value": {
                                    "averageeCPM": 12.55,
                                    "completedCount": 7
                                }, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}
                            }, {
                                "value": {"averageeCPM": 8.8426, "completedCount": 30},
                                "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}
                            }]
                        })]);
                    console.log(urlRoot + "average?" + apiKey + "&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B" + filter + "%5D&" + timeframe);

                };
                listenForKeen("");
                listenForKeen("%7B%22property_name%22%3A%22ad_provider_name%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B%22UnityAds%22%2C%22HyprMarketplace%22%5D%7D");
                // Set up the mock http service responses
                $httpBackend = _$httpBackend_;

                configRequestHandler = $httpBackend.when('GET', '/distributors/undefined/analytics/info')
                    .respond({
                        "distributorID": 620798327,
                        "adProviders": [{"name": "Unity Ads", "id": "UnityAds"}, {
                            "name": "HyprMarketplace",
                            "id": "HyprMarketplace"
                        }, {"name": "Vungle", "id": "Vungle"}, {"name": "AppLovin", "id": "AppLovin"}],
                        "apps": [{"id": "578021165", "distributorID": 620798327, "name": "Zombie Game"}],
                        "keenProject": "5512efa246f9a74b786bc7d1",
                        "scopedKey": "D8DD8FDF000323000448F"
                    });

                scope = $rootScope.$new();
                $window = _$window_;
                scope.debounceWait = 0;
                scope.defaultStartDate = new Date(moment.utc("2015-04-03T00:00:00.000Z").format());
                scope.defaultEndDate = new Date(moment.utc("2015-04-15T00:00:00.000Z").format());
                angular.element(document.body).append('<input id="start-date" />');
                testCont = $controller('AnalyticsController', {$scope: scope});
                $httpBackend.flush();
            }));

            beforeEach(function (done) {
                setInterval(function () {
                    if (scope.currentlyUpdating === false) {
                        done();
                    }
                }, 100);
            });

            afterEach(function () {
                $httpBackend.verifyNoOutstandingExpectation();
                $httpBackend.verifyNoOutstandingRequest();
                server.restore();
                angular.element('#start-date').remove();
            });

            it('should have the ecpmMetric correct', function () {
                expect(scope.analyticsData.ecpmMetric).toEqual("<sup>$</sup>6<sup>.95</sup>");
            });

            it('should have the correct table data', function () {
                var completions = 48;
                var impressions = 203;
                var averageEcpm = 6.44;
                var estimatedRevenue = scope.calculateDayRevenue(completions, impressions, averageEcpm);
                expect(scope.analyticsData.revenueTable.length).toEqual(2);
                expect(scope.analyticsData.revenueTable[0].averageeCPM).toEqual("$" + averageEcpm.toString());
                expect(scope.analyticsData.revenueTable[0].date).toEqual("Apr 11, 2015");
                expect(scope.analyticsData.revenueTable[0].completedCount).toEqual(completions);
                expect(scope.analyticsData.revenueTable[0].estimatedRevenue).toEqual("$" + filter("monetaryFormat")(estimatedRevenue));
                expect(scope.analyticsData.revenueTable[0].fillRate).toEqual("29%");
                expect(scope.analyticsData.revenueTable[0].impressions).toEqual(impressions);
                expect(scope.analyticsData.revenueTable[0].requests).toEqual(597);
            });

            it('should have the revenueByDayMetric correct', function () {
                expect(scope.analyticsData.revenueByDayMetric).toEqual("<sup>$</sup>1<sup>.31</sup>");
            });
        });
    });
});
