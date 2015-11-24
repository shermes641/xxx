describe('WaterfallControllerSpec', function() {
    beforeEach(module('MediationModule'));

    describe('waterfallPage', function() {
        var waterfallInfo;
        beforeEach(inject(function($rootScope, $controller, $compile, $httpBackend) {
            var appID = "62484878";
            waterfallInfo = {
                "distributorID":620798327,
                "waterfall":{
                    "id":"62484878",
                    "appID":appID,
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
                    {"name":"HyprMarketplace","waterfallAdProviderID":5,"cpm":20.0,"active":false,"waterfallOrder":null,"unconfigured":true,"newRecord":false,"configurable":false,"pending":true},
                    {"name":"AppLovin","waterfallAdProviderID":4,"cpm":null,"active":false,"waterfallOrder":null,"unconfigured":true,"newRecord":true,"configurable":true,"pending":false}
                ],
                "appsWithWaterfalls":[
                    {"id":"62484878","active":true,"distributorID":620798327,"name":"Test App 3","waterfallID":62484878},
                    {"id":"265019496","active":true,"distributorID":620798327,"name":"Really really really really long app name","waterfallID":1000471524},
                    {"id":"1007066320","active":true,"distributorID":620798327,"name":"Test App2","waterfallID":1007066320}
                ],
                "generationNumber":50
            };

            scope = $rootScope.$new();
            httpBackend = $httpBackend;

            routeParams = { distributorID: 456, waterfallID: 1007066320 };
            testCont = $controller('WaterfallController', {$scope: scope, $routeParams: routeParams});

            // Mock waterfall_info response
            httpBackend.when("GET", "/distributors/" + routeParams.distributorID + "/waterfalls/" + routeParams.waterfallID + "/waterfall_info").respond(waterfallInfo);

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

            scope.showModal = function(){};
            scope.editAppID = appID;
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

        describe('App Settings Modal', function() {
            var updateApp = function(newGeneration) {
                // Stub app update response
                httpBackend.expectPOST("/distributors/" + routeParams.distributorID + "/apps/" + scope.editAppID).respond({
                    "generationNumber":newGeneration,
                    "status":"success",
                    "message":"App updated successfully"
                }, scope.data);
                scope.submitEditApp(scope.form);
                httpBackend.flush();
            };

            it('should persist the new generation number for the currently selected app when the app update response is successful', function() {
                var newGeneration = scope.generationNumber + 1;
                waterfallInfo.generationNumber = newGeneration;
                updateApp(newGeneration);
                expect(scope.generationNumber).toEqual(newGeneration);
            });

            it('should not persist the new generation number when editing an app which is not currently selected on the waterfall page', function() {
                var originalGeneration = scope.generationNumber;
                waterfallInfo.generationNumber = originalGeneration;
                scope.editAppID = 9876;
                updateApp(originalGeneration + 1);
                expect(scope.generationNumber).toEqual(originalGeneration);
            });
        });

        describe('waterfall status updates', function() {
            it('should update to paused mode when toggled', function() {
                spyOn(scope, 'updateWaterfall');
                scope.activatePausedMode();
                expect(scope.waterfallData.waterfall.paused).toEqual(true);
                expect(scope.waterfallData.waterfall.testMode).toEqual(false);
                scope.activateLiveMode();
                expect(scope.waterfallData.waterfall.testMode).toEqual(false);
                expect(scope.waterfallData.waterfall.paused).toEqual(false);
                expect(scope.updateWaterfall).toHaveBeenCalled();
            });

            it('should not toggle into paused mode when the waterfall is currently paused', function() {
                spyOn(scope, 'updateWaterfall');
                scope.waterfallData.waterfall.paused = true;
                scope.activatePausedMode();
                expect(scope.updateWaterfall).not.toHaveBeenCalled();
            });

            it('should update to test mode when toggled', function() {
                spyOn(scope, 'showModal');
                scope.confirmTestMode();
                expect(scope.waterfallData.waterfall.testMode).toEqual(true);
                expect(scope.waterfallData.waterfall.paused).toEqual(false);
                scope.activateLiveMode();
                expect(scope.waterfallData.waterfall.testMode).toEqual(false);
                expect(scope.waterfallData.waterfall.paused).toEqual(false);
                expect(scope.showModal).toHaveBeenCalled();
            });

            it('should not toggle into test mode when the waterfall is currently in test mode', function() {
                spyOn(scope, 'showModal');
                scope.waterfallData.waterfall.testMode = true;
                scope.activateTestMode();
                expect(scope.showModal).not.toHaveBeenCalled();
            });

            it('should update live mode  when toggled', function() {
                spyOn(scope, 'updateWaterfall');
                scope.activateLiveMode();
                expect(scope.waterfallData.waterfall.paused).toEqual(false);
                expect(scope.waterfallData.waterfall.testMode).toEqual(false);
                scope.confirmTestMode();
                expect(scope.waterfallData.waterfall.testMode).toEqual(true);
                expect(scope.waterfallData.waterfall.paused).toEqual(false);
                expect(scope.updateWaterfall).toHaveBeenCalled();
            });

            it('should not toggle into live mode when the waterfall is currently in live mode', function() {
                spyOn(scope, 'updateWaterfall');
                scope.waterfallData.waterfall.testMode = false;
                scope.waterfallData.waterfall.paused = false;
                scope.activateLiveMode();
                expect(scope.updateWaterfall).not.toHaveBeenCalled();
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

        it('should active and deactive ad providers correctly', function() {
            var activeProviders = scope.providersByActive(true);
            expect(activeProviders[0].name).toEqual("AdColony");
            expect(activeProviders[1].name).toEqual("Vungle");
            var inactiveProviders = scope.providersByActive(false);
            expect(inactiveProviders[0].name).toEqual("HyprMarketplace");
            expect(inactiveProviders[1].name).toEqual("AppLovin");

            scope.toggleWAPStatus(activeProviders[0]);
            inactiveProviders = scope.providersByActive(false);
            expect(inactiveProviders[0].name).toEqual("AdColony");
            expect(inactiveProviders[1].name).toEqual("HyprMarketplace");
            expect(inactiveProviders[2].name).toEqual("AppLovin");
            activeProviders = scope.providersByActive(true);
            expect(activeProviders[0].name).toEqual("Vungle");

            scope.toggleWAPStatus(inactiveProviders[2]);
            inactiveProviders = scope.providersByActive(false);
            expect(inactiveProviders[0].name).toEqual("AdColony");
            expect(inactiveProviders[1].name).toEqual("HyprMarketplace");
            activeProviders = scope.providersByActive(true);
            expect(activeProviders[0].name).toEqual("Vungle");
            expect(activeProviders[1].name).toEqual("AppLovin");
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

    describe('validateRequiredParamFormat', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input required validate-required-param-format="{{param.value}}" required-param-data-type="{{param.dataType}}" ng-minlength="{{param.minLength}}" name="{{param.key}}" type="text" ng-model="param.value"></input>' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.param = { value: '', dataType: 'Array', minLength: 1, key: 'zone IDs' };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if an element of the array is empty', function() {
            var testValues = ["zone1, ,zone2", "zone1,    ,zone2", "zone1,", "zone1,,zone2", ",zone1,zone2", "", " "];
            for(var i = 0; i < testValues.length; i++) {
                scope.param.value = testValues[i];
                scope.$digest();
                expect(form[scope.param.key].$error.validateRequiredParamFormat).toEqual(true);
                expect(form[scope.param.key].$valid).toEqual(false);
            }
        });

        it('should be valid if none of the elements in the array are empty', function() {
            var testValues = ["zone1,zone2", "zone1, zone2", "zone1,    zone2", "zone1", "zone1,zone2, zone3"];
            for(var i = 0; i < testValues.length; i++) {
                scope.param.value = testValues[i];
                scope.$digest();
                expect(form[scope.param.key].$error).toEqual({});
                expect(form[scope.param.key].$valid).toEqual(true);
            }
        });
    });
});
