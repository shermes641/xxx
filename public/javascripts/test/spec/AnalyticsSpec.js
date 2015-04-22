
describe('AnalyticsController', function() {
    beforeEach(module('MediationModule'));

    describe('AnalyticsController', function(){
        var scope, testCont, $window, $httpBackend, configRequestHandler, server;
        server = sinon.fakeServer.create();
        server.respondImmediately = true;
        server.respondWith("GET", "http://api.keen.io/3.0/projects/5512efa246f9a74b786bc7d1/queries/average?api_key=D8DD8FDF000323000448F&event_collection=ad_completed&target_property=ad_provider_eCPM&filters=%5B%5D&timeframe=%7B%22start%22%3A%222015-04-02T20%3A00%3A00%2B00%3A00%22%2C%22end%22%3A%222015-04-15T20%3A00%3A00%2B00%3A00%22%7D&timezone=-14400", [ 200, { "Content-Type": "application/json" }, JSON.stringify({"result": 5.01123595505618}) ]);
        server.respondWith("GET", "http://api.keen.io/3.0/projects/5512efa246f9a74b786bc7d1/queries/count?api_key=D8DD8FDF000323000448F&event_collection=mediate_availability_response_true&interval=daily&filters=%5B%5D&timeframe=%7B%22start%22%3A%222015-04-02T20%3A00%3A00%2B00%3A00%22%2C%22end%22%3A%222015-04-15T20%3A00%3A00%2B00%3A00%22%7D&timezone=-14400", [ 200, { "Content-Type": "application/json" }, JSON.stringify({"result": [{"value": 147, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}}, {"value": 173, "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}}]}) ]);
        server.respondWith("GET", "http://api.keen.io/3.0/projects/5512efa246f9a74b786bc7d1/queries/multi_analysis?api_key=D8DD8FDF000323000448F&event_collection=ad_completed&interval=daily&analyses=%7B%22completedCount%22%3A%7B%22analysis_type%22%3A%22count%22%7D%2C%22averageeCPM%22%3A%7B%22analysis_type%22%3A%22average%22%2C%22target_property%22%3A%22ad_provider_eCPM%22%7D%7D&filters=%5B%5D&timeframe=%7B%22start%22%3A%222015-04-02T20%3A00%3A00%2B00%3A00%22%2C%22end%22%3A%222015-04-15T20%3A00%3A00%2B00%3A00%22%7D&timezone=-14400", [ 200, { "Content-Type": "application/json" }, JSON.stringify({"result": [{"value": {"averageeCPM": 4.63265306122449, "completedCount": 49}, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}}, {"value": {"averageeCPM": 5.475, "completedCount": 40}, "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}}]}) ]);
        server.respondWith("GET", "http://api.keen.io/3.0/projects/5512efa246f9a74b786bc7d1/queries/count?api_key=D8DD8FDF000323000448F&event_collection=mediate_availability_requested&interval=daily&filters=%5B%5D&timeframe=%7B%22start%22%3A%222015-04-02T20%3A00%3A00%2B00%3A00%22%2C%22end%22%3A%222015-04-15T20%3A00%3A00%2B00%3A00%22%7D&timezone=-14400", [ 200, { "Content-Type": "application/json" }, JSON.stringify({"result": [{"value": 400, "timeframe": {"start": "2015-04-10T00:00:00.000Z", "end": "2015-04-11T00:00:00.000Z"}}, {"value": 597, "timeframe": {"start": "2015-04-11T00:00:00.000Z", "end": "2015-04-12T00:00:00.000Z"}}]}) ]);

        beforeEach(inject(function($rootScope, $controller, _$window_, _$httpBackend_) {
            // Set up the mock http service responses
            $httpBackend = _$httpBackend_;

            configRequestHandler = $httpBackend.when('GET', '/distributors/undefined/analytics/info')
                .respond({"distributorID":620798327,"adProviders":[{"name":"AdColony","id":1},{"name":"HyprMarketplace","id":2},{"name":"Vungle","id":3},{"name":"AppLovin","id":4}],"apps":[{"id":"578021165","distributorID":620798327,"name":"Zombie Game"}],"keenProject":"5512efa246f9a74b786bc7d1","scopedKey":"D8DD8FDF000323000448F"});

            scope = $rootScope.$new();
            $window = _$window_;
            scope.debounceWait = 0;
            testCont = $controller('AnalyticsController', {$scope: scope});
            scope.defaultStartDate = new Date("2015-04-03T00:00:00.000Z");
            scope.defaultEndDate = new Date("2015-04-15T00:00:00.000Z");
            angular.element(document.body).append('<input id="start_date" /><input id="end_date" />');

            $httpBackend.flush();
        }));

        beforeEach(function(done) {
            setInterval(function() {
                if(scope.currentlyUpdating === false){
                    done();
                }
            }, 100);
        });

        afterEach(function() {
            $httpBackend.verifyNoOutstandingExpectation();
            $httpBackend.verifyNoOutstandingRequest();
            angular.element('#start_date, #end_date').remove();
        });

        it('should be defined', function(){
            expect(testCont).toBeDefined();
        });

        it('should get config correctly', function(){
            expect(scope.scopedKey).toEqual("D8DD8FDF000323000448F");
            expect(scope.keenProject).toEqual("5512efa246f9a74b786bc7d1");

            expect(scope.filters.ad_providers.available.length).toEqual(4);
            expect(scope.filters.apps.available[0].name).toEqual("Zombie Game");
            expect(scope.filters.countries.available.length).toEqual(191);
        });

        it('should have the ecpmMetric correct', function(){
            expect(scope.analyticsData.ecpmMetric).toEqual("<sup>$</sup>5<sup>.01</sup>");
        });

        it('should have the correct table data', function(){
            expect(scope.analyticsData.revenueTable.length).toEqual(2);
            expect(scope.analyticsData.revenueTable[0].averageeCPM).toEqual("$5.47");
            expect(scope.analyticsData.revenueTable[0].date).toEqual("Apr 11, 2015");
            expect(scope.analyticsData.revenueTable[0].completedCount).toEqual(40);
            expect(scope.analyticsData.revenueTable[0].estimatedRevenue).toEqual("$0.21");
            expect(scope.analyticsData.revenueTable[0].fillRate).toEqual("29%");
            expect(scope.analyticsData.revenueTable[0].impressions).toEqual(173);
            expect(scope.analyticsData.revenueTable[0].requests).toEqual(597);
        });

        it('should have the fillRateMetric correct', function(){
            expect(scope.analyticsData.fillRateMetric).toEqual("32%");
        });

        it('should have the revenueByDayMetric correct', function(){
            expect(scope.analyticsData.revenueByDayMetric).toEqual("<sup>$</sup>0<sup>.22</sup>");
        });

        it('should be initalized correctly', function(){
            expect(scope.subHeader).toEqual('assets/templates/sub_header.html');
            expect(scope.page).toEqual('analytics');
            expect(scope.currentlyUpdating).toEqual(false);
            expect(scope.updatingStatus).toEqual("Updating...");
            expect(scope.keenTimeout).toEqual(45000);
        });

        it('should reject invalid dates', function(){
            expect(scope.isValidDate(new Date("FAKE DATE"))).toEqual(false);
            expect(scope.isValidDate(new Date)).toEqual(true);
        });

        it('should have moment.js available', function(){
            expect(moment).toBeDefined();
        });

        it('should parse the datepicker dates correctly if user is in EST/EDT', function(){
            // Should return Feb 16 to March 16
            var start = "Sun Feb 15 2015 19:00:00 GMT-0500 (EST)";
            var end = "Sun Mar 15 2015 20:00:00 GMT-0400 (EDT)";
            expect(moment(start).utc().format("YYYY-MM-DD")).toEqual("2015-02-16");
            expect(moment(end).utc().format("YYYY-MM-DD")).toEqual("2015-03-16");
            expect(moment(start).utc().format()).toEqual("2015-02-16T00:00:00+00:00");
            expect(moment(end).utc().add(1, 'days').format()).toEqual("2015-03-17T00:00:00+00:00");
        });

        it('should parse the datepicker dates and return the same result regardless of the users timezone', function(){
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

        it('should trigger analytics update on resize', function(){
            scope.currentlyUpdating = false;
            $window.dispatchEvent(new Event('resize'));
            expect(scope.currentlyUpdating).toEqual(true);
        });

        it('should calculate revenue per day correctly', function(){
            expect(scope.calculateDayRevenue(1000, 1)).toEqual(1);
            expect(scope.calculateDayRevenue(500, 1)).toEqual(0.5);
            expect(scope.calculateDayRevenue(2000, 1)).toEqual(2);
            expect(scope.calculateDayRevenue(1000, 0.5)).toEqual(0.5);
            expect(scope.calculateDayRevenue(10000, 0.1)).toEqual(1);
            expect(scope.calculateDayRevenue(100, 50)).toEqual(5);
            expect(scope.calculateDayRevenue(10, 50)).toEqual(0.5);
        });
    });
});