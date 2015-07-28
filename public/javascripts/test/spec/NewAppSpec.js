describe('NewAppSpec', function() {
    beforeEach(module('MediationModule'));


    describe('CreateNewApp - Waterfall', function() {
        var scope, testCont, compile, form, newAppResponseMock;

        beforeEach(inject(function($rootScope, $controller, $compile, $httpBackend) {
            scope = $rootScope.$new();
            httpBackend = $httpBackend;

            // Mock waterfall_info response
            httpBackend.when("GET", "/distributors/456/waterfalls/undefined/waterfall_info").respond({
                waterfall: {
                    appName: "test app",
                    appID: 123
                },
                generationNumber: 1,
                waterfallAdProviderList: []
            });

            // Mock new app response
            newAppResponseMock = httpBackend.when("POST", "/distributors/456/apps");

            element = angular.element(
                '<form name="form.newAppForm">' +
                '<input required type="text" ng-model="newApp.appName" ng-model-options="{updateOn: \'blur\'}" id="newAppName" name="appName" placeholder="name" label="*App Name">' +
                '<input required-integer type="text" ng-model="newApp.exchangeRate" ng-model-options="{updateOn: \'blur\'}" id="newAppExchangeRate" name="exchangeRate" placeholder="e.g. 100">' +
                '<input type="text" ng-model="newApp.rewardMin" ng-model-options="{updateOn: \'blur\'}" id="newAppRewardMin" name="rewardMin">' +
                '<input greater-than-or-equal-to="{{data.rewardMin}}" type="text" ng-model="newApp.rewardMax" ng-model-options="{updateOn: \'blur\'}" id="newAppRewardMax" name="rewardMax">' +
                '</form>'
            );

            scope.newApp = {rewardMax: undefined, rewardMin: undefined, exchangeRate: undefined, appName: undefined};
            testCont = $controller('WaterfallController', {$scope: scope, $routeParams: { distributorID: 456 }});

            scope.showModal = function(){};
            compile = $compile;
            compile(element)(scope);
            form = scope.form.newAppForm;
        }));

        it('should have the waterfall controller defined', function() {
            expect(testCont).toBeDefined();
        });

        it('should be initalized correctly', function() {
            expect(scope.showNewAppModal).toEqual(undefined);
            expect(scope.newAppModal).toEqual('assets/templates/apps/newAppModal.html');
            expect(scope.newAppModalTitle).toEqual("Create New App");
            expect(scope.errors).toEqual({});
        });

        it('should not be able to submit twice until we receive a response from the server (200)', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.$digest();

            newAppResponseMock.respond(200, { data: "" })

            scope.submitNewApp(form);
            expect(form.submitting).toEqual(undefined);
            httpBackend.flush();

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should not be able to submit twice until we receive a response from the server (400)', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.newApp.appName = "test";
            scope.$digest();

            newAppResponseMock.respond(400, { data: "" });
            httpBackend.flush();

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should be able to submit again after response is 500', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.$digest();

            newAppResponseMock.respond(500, { data: "" });
            httpBackend.flush();

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should handle error if server returns 400', function(){
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.newApp.appName = "test";
            scope.$digest();

            newAppResponseMock.respond(400, { status:"error", fieldName:"appName", message:"You already have an App with the same name.  Please choose a unique name for your new App." });
            httpBackend.flush();

            form.$valid = true;
            scope.submitNewApp(form);
            httpBackend.flush();
            expect(scope.errors.appName).toEqual("You already have an App with the same name.  Please choose a unique name for your new App.");
            expect(scope.errors.appNameClass).toEqual("error");
        });
    });

    describe('CreateNewApp - First App', function() {
        var scope, testCont, compile, form, newAppResponseMock;

        beforeEach(inject(function($rootScope, $controller, $compile, $httpBackend) {
            scope = $rootScope.$new();
            httpBackend = $httpBackend;

            // Mock new app response
            newAppResponseMock = httpBackend.when("POST", "/distributors/456/apps");

            element = angular.element(
                '<form name="form.newAppForm">' +
                    '<input required type="text" ng-model="newApp.appName" ng-model-options="{updateOn: \'blur\'}" id="newAppName" name="appName" placeholder="name" label="*App Name">' +
                    '<input required-integer type="text" ng-model="newApp.exchangeRate" ng-model-options="{updateOn: \'blur\'}" id="newAppExchangeRate" name="exchangeRate" placeholder="e.g. 100">' +
                    '<input type="text" ng-model="newApp.rewardMin" ng-model-options="{updateOn: \'blur\'}" id="newAppRewardMin" name="rewardMin">' +
                    '<input greater-than-or-equal-to="{{data.rewardMin}}" type="text" ng-model="newApp.rewardMax" ng-model-options="{updateOn: \'blur\'}" id="newAppRewardMax" name="rewardMax">' +
                    '</form>'
            );

            scope.newApp = {rewardMax: undefined, rewardMin: undefined, exchangeRate: undefined, appName: undefined};
            testCont = $controller('NewAppsController', {$scope: scope, $routeParams: { distributorID: 456 }});

            compile = $compile;
            compile(element)(scope);
            form = scope.form.newAppForm;
            scope.testing = true;
        }));

        it('should have the waterfall controller defined', function() {
            expect(testCont).toBeDefined();
        });

        it('should be initalized correctly', function() {
            expect(scope.showNewAppModal).toEqual(undefined);
            expect(scope.newAppModalTitle).toEqual("Welcome to hyprMediate!");
        });

        it('should not be able to submit twice until we receive a response from the server (200)', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.$digest();

            newAppResponseMock.respond(200, { data: "" })

            scope.submitNewApp(form);
            expect(form.submitting).toEqual(undefined);

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should not be able to submit twice until we receive a response from the server (400)', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.newApp.appName = "test";
            scope.$digest();

            newAppResponseMock.respond(400, { data: "" });

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should be able to submit again after response is 500', function() {
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.$digest();

            newAppResponseMock.respond(500, { data: "" });

            form.$valid = true;
            scope.submitNewApp(form);
            // Before new app response, form should be in submitting state
            expect(form.submitting).toEqual(true);
            httpBackend.flush();
            // Only after request has completed should you be able to submit again
            expect(form.submitting).toEqual(false);
        });

        it('should handle error if server returns 400', function(){
            scope.newApp.rewardMax = "123";
            scope.newApp.rewardMin = "13";
            scope.newApp.exchangeRate = "1";
            scope.newApp.appName = "test";
            scope.$digest();

            newAppResponseMock.respond(400, { status:"error", fieldName:"appName", message:"You already have an App with the same name.  Please choose a unique name for your new App." });

            form.$valid = true;
            scope.submitNewApp(form);
            httpBackend.flush();
            expect(scope.errors.appName).toEqual("You already have an App with the same name.  Please choose a unique name for your new App.");
            expect(scope.errors.appNameClass).toEqual("error");
        });
    });
});
