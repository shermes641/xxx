describe('WaterfallControllerSpec', function() {
    beforeEach(module('MediationModule'));

    describe('waterfallPageSetup', function() {
        beforeEach(inject(function($rootScope, $controller, $compile, $httpBackend) {
            scope = $rootScope.$new();
            httpBackend = $httpBackend;

            // Mock waterfall_info response
            httpBackend.when("GET", "/distributors/456/waterfalls/undefined/waterfall_info").respond({
                "distributorID":620798327,
                "waterfall":{
                    "id":"62484878",
                    "appID":"62484878",
                    "name":"Test App 3",
                    "token":"4add175e-6a1e-4e33-adc8-f39492b953bc",
                    "optimizedOrder":true,
                    "testMode":false,
                    "paused":false,
                    "appName":"Test App 3",
                    "appToken":"49126db4-876a-4513-9ed3-8f080eb6afb8"},
                "waterfallAdProviderList":[
                    {"name":"AdColony","waterfallAdProviderID":40,"cpm":1.0,"active":true,"waterfallOrder":0,"unconfigured":false,"newRecord":false,"configurable":true,"pending":false},
                    {"name":"Vungle","waterfallAdProviderID":41,"cpm":0.5,"active":true,"waterfallOrder":1,"unconfigured":false,"newRecord":false,"configurable":true,"pending":false},
                    {"name":"HyprMarketplace&#8480","waterfallAdProviderID":5,"cpm":20.0,"active":false,"waterfallOrder":null,"unconfigured":true,"newRecord":false,"configurable":false,"pending":true},
                    {"name":"AppLovin","waterfallAdProviderID":4,"cpm":null,"active":false,"waterfallOrder":null,"unconfigured":true,"newRecord":true,"configurable":true,"pending":false}
                ],
                "appsWithWaterfalls":[
                    {"id":"62484878","active":true,"distributorID":620798327,"name":"Test App 3","waterfallID":62484878},
                    {"id":"265019496","active":true,"distributorID":620798327,"name":"Really really really really long app name","waterfallID":1000471524},
                    {"id":"1007066320","active":true,"distributorID":620798327,"name":"Test App2","waterfallID":1007066320}
                ],
                "generationNumber":50
            });

            element = angular.element(
                '<form name="form">' +
                    '<div class="onoffswitch switch-left">' +
                    '<input ng-model="data.reportingActive" type="checkbox" name="reporting-active" class="onoffswitch-checkbox" id="reporting-active-switch">' +
                    '<label class="onoffswitch-label" for="reporting-active-switch">' +
                    '<span class="onoffswitch-inner"></span>' +
                    '<span class="onoffswitch-switch"></span>' +
                    '</label>' +
                    '</div>' +
                    '<input reporting-required="{{data.reportingActive}}" name="reportKey" type="text" ng-model="data.reportKey"></input>' +
                    '</form>'
            );

            testCont = $controller('WaterfallController', {$scope: scope, $routeParams: { distributorID: 456 }});

            scope.showModal = function(){};
            scope.data = { reportingActive: false };
            $compile(element)(scope);
            form = scope.form;
            httpBackend.flush();
        }));

        it('should have the waterfall controller defined', function() {
            expect(testCont).toBeDefined();
        });

        it('should have templates set correctly', function() {
            expect(scope.appList).toEqual("assets/templates/waterfalls/appList.html");
            expect(scope.editAppModal).toEqual("assets/templates/apps/editAppModal.html");
            expect(scope.editWaterfallAdProviderModal).toEqual("assets/templates/waterfall_ad_providers/edit.html");
            expect(scope.newAppModal).toEqual("assets/templates/apps/newAppModal.html");
            expect(scope.subHeader).toEqual("assets/templates/sub_header.html");
            expect(scope.testModeConfirmationModal).toEqual("assets/templates/waterfalls/test_mode_confirmation.html");
        });

        it('should be in the correct state', function() {
            expect(scope.showCodeBlock).toEqual(false);
            expect(scope.showTestModeConfirmationModal).toEqual(false);
            expect(scope.showWaterfallAdProviderModal).toEqual(false);
            expect(scope.modalShown).toEqual(false);
            expect(scope.disableTestModeToggle).toEqual(false);
            expect(scope.adProviderModalShown).toEqual(false);
            expect(scope.waterfallInfoCallComplete).toEqual(true);
            expect(scope.errors).toEqual({});
        });

        it('should have the waterfall data configured correctly', function() {
            var waterfallData = scope.waterfallData;
            expect(waterfallData.appsWithWaterfalls.length).toEqual(3);
            expect(waterfallData.appsWithWaterfalls[0].name).toEqual("Test App 3");
            expect(waterfallData.appsWithWaterfalls[0].distributorID).toEqual(620798327);
            expect(waterfallData.appsWithWaterfalls[0].id).toEqual("62484878");
            expect(waterfallData.appsWithWaterfalls[0].active).toEqual(true);
            expect(waterfallData.appsWithWaterfalls[0].waterfallID).toEqual(62484878);

            expect(waterfallData.waterfall.appName).toEqual("Test App 3");
            expect(waterfallData.waterfall.appToken).toEqual("49126db4-876a-4513-9ed3-8f080eb6afb8");
            expect(waterfallData.waterfall.id).toEqual("62484878");
            expect(waterfallData.waterfall.optimizedOrder).toEqual(true);
            expect(waterfallData.waterfall.paused).toEqual(false);
            expect(waterfallData.waterfall.testMode).toEqual(false);
            expect(waterfallData.waterfall.paused).toEqual(false);
            expect(waterfallData.waterfall.token).toEqual("4add175e-6a1e-4e33-adc8-f39492b953bc");

            expect(waterfallData.waterfallAdProviderList.length).toEqual(4);
            expect(waterfallData.waterfallAdProviderList[0].active).toEqual(true);
            expect(waterfallData.waterfallAdProviderList[0].configurable).toEqual(true);
            expect(waterfallData.waterfallAdProviderList[0].cpm).toEqual(1);
            expect(waterfallData.waterfallAdProviderList[0].name).toEqual("AdColony");
            expect(waterfallData.waterfallAdProviderList[0].newRecord).toEqual(false);
            expect(waterfallData.waterfallAdProviderList[0].pending).toEqual(false);
            expect(waterfallData.waterfallAdProviderList[0].unconfigured).toEqual(false);
            expect(waterfallData.waterfallAdProviderList[0].waterfallAdProviderID).toEqual(40);
            expect(waterfallData.waterfallAdProviderList[0].waterfallOrder).toEqual(0);
        });

        it('should update paused when toggled', function() {
            scope.togglePaused();
            expect(scope.waterfallData.waterfall.paused).toEqual(true);
            scope.togglePaused();
            expect(scope.waterfallData.waterfall.paused).toEqual(false);
        });

        it('should update code block when toggled', function() {
            scope.toggleCodeBlock();
            expect(scope.showCodeBlock).toEqual(true);
            scope.toggleCodeBlock();
            expect(scope.showCodeBlock).toEqual(false);
        });

        it('should modal cancel and confirm should toggle test mode correctly', function() {
            scope.confirmTestMode();
            expect(scope.waterfallData.waterfall.testMode).toEqual(true);
            scope.cancelTestMode();
            expect(scope.waterfallData.waterfall.testMode).toEqual(false);
        });
    });

    describe('validateEcpm', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input validate-ecpm type="text" ng-model="data.eCPM" id="eCPM" name="eCPM">' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = {};
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if eCPM is a negative number', function() {
            scope.data.eCPM = '-1';
            scope.$digest();
            expect(form.eCPM.$valid).toEqual(false);
        });

        it('should be invalid if eCPM is blank', function() {
            scope.data.eCPM = ' ';
            scope.$digest();
            expect(form.eCPM.$valid).toEqual(false);
        });

        it('should be valid if eCPM is 0', function() {
            scope.data.eCPM = '0';
            scope.$digest();
            expect(form.eCPM.$valid).toEqual(true);
        });

        it('should be valid if eCPM is a decimal number greater than 0', function() {
            scope.data.eCPM = '20.00';
            scope.$digest();
            expect(form.eCPM.$valid).toEqual(true);
        });
    });

    describe('reportingRequired', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<div class="onoffswitch switch-left">' +
                '<input ng-model="data.reportingActive" type="checkbox" name="reporting-active" class="onoffswitch-checkbox" id="reporting-active-switch">' +
                '<label class="onoffswitch-label" for="reporting-active-switch">' +
                '<span class="onoffswitch-inner"></span>' +
                '<span class="onoffswitch-switch"></span>' +
                '</label>' +
                '</div>' +
                '<input reporting-required="{{data.reportingActive}}" name="reportKey" type="text" ng-model="data.reportKey"></input>' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = { reportingActive: false };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if reporting is enabled and the reporting input is blank', function() {
            scope.data.reportingActive = true;
            scope.$digest();
            expect(form.reportKey.$valid).toEqual(false);
        });

        it('should be valid if reporting is enabled and the reporting input is not blank', function() {
            scope.data.reportingActive = true;
            scope.data.reportKey = 'some key';
            scope.$digest();
            expect(form.reportKey.$valid).toEqual(true);
        });

        it('should be valid if reporting is not enabled and the reporting input is blank', function() {
            scope.$digest();
            expect(form.reportKey.$valid).toEqual(true);
        });

        it('should be valid if reporting is not enabled and the reporting input is not blank', function() {
            scope.data.reportKey = 'some key';
            scope.$digest();
            expect(form.reportKey.$valid).toEqual(true);
        });
    });
});
