
describe('AnalyticsController', function() {
    beforeEach(module('MediationModule'));


    describe('AnalyticsController', function(){
        var scope, testCont, $window;

        beforeEach(inject(function($rootScope, $controller, _$window_) {
            scope = $rootScope.$new();
            $window = _$window_;
            testCont = $controller('AnalyticsController', {$scope: scope});
        }));

        it('should be defined', function(){
            expect(testCont).toBeDefined();
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
            expect(scope.currentlyUpdating).toEqual(false);
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