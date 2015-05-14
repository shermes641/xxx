describe('WaterfallControllerSpec', function() {
    beforeEach(module('MediationModule'));

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
