describe('EditAppSpec', function() {
    beforeEach(module('MediationModule'));

    describe('GreaterThanOrEqualTo', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input type="text" ng-model="data.rewardMin" ng-model-options="{updateOn: \'blur\'}" id="rewardMin" name="rewardMin">' +
                '<input greater-than-or-equal-to="{{data.rewardMin}}" type="text" ng-model="data.rewardMax" ng-model-options="{updateOn: \'blur\'}" id="rewardMax" name="rewardMax">' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = { rewardMax: undefined, rewardMin: undefined };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if rewardMax is less than rewardMax', function() {
            scope.data.rewardMin = '10';
            scope.data.rewardMax = '1';
            scope.$digest();
            expect(form.rewardMax.$valid).toEqual(false);
        });

        it('should be valid if rewardMax is greater than rewardMin', function() {
            scope.data.rewardMin = '1';
            scope.data.rewardMax = '10';
            scope.$digest();
            expect(form.rewardMax.$valid).toEqual(true);
        });

        it('should be valid if rewardMax is empty', function() {
            scope.data.rewardMin = '1';
            scope.data.rewardMax = undefined;
            scope.$digest();
            expect(form.rewardMax.$valid).toEqual(true);
        });
    });

    describe('LessThanOrEqualTo', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input less-than-or-equal-to="{{data.rewardMax}}" type="text" ng-model="data.rewardMin" ng-model-options="{updateOn: \'blur\'}" id="rewardMin" name="rewardMin">' +
                '<input type="text" ng-model="data.rewardMax" ng-model-options="{updateOn: \'blur\'}" id="rewardMax" name="rewardMax">' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = { rewardMax: undefined, rewardMin: undefined };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if rewardMin is greater than rewardMax', function() {
            scope.data.rewardMin = '10';
            scope.data.rewardMax = '1';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(false);
        });

        it('should be valid if rewardMin is less than rewardMax', function() {
            scope.data.rewardMin = '1';
            scope.data.rewardMax = '10';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(true);
        });

        it('should be valid if rewardMax is empty', function() {
            scope.data.rewardMin = '1';
            scope.data.rewardMax = undefined;
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(true);
        });
    });

    describe('requiredInteger', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input required-integer type="text" ng-model="data.rewardMin" ng-model-options="{updateOn: \'blur\'}" id="rewardMin" name="rewardMin">' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = { rewardMin: null };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if the field is set to some character other than a number', function() {
            scope.data.rewardMin = 'some string';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(false);
        });

        it('should be invalid if the field is set to a decimal value', function() {
            scope.data.rewardMin = '1.5';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(false);
        });

        it('should be invalid if the field is set a number less than 1', function() {
            scope.data.rewardMin = '0';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(false);
        });

        it('should be valid if the field is set an integer, greater than or equal to 1', function() {
            scope.data.rewardMin = '1';
            scope.$digest();
            expect(form.rewardMin.$valid).toEqual(true);
        });
    });

    describe('callbackValidator', function() {
        var scope, form, element;

        beforeEach(inject(function($rootScope, $compile) {
            element = angular.element(
                '<form name="form">' +
                '<input type="checkbox" ng-model="data.serverToServerEnabled" id="serverToServerEnabled" name="serverToServerEnabled">' +
                '<input callback-validator="{{data.serverToServerEnabled}}" type="text" ng-model="data.callbackURL" ng-model-options="{updateOn: \'blur\'}" id="callbackURL" name="callbackURL">' +
                '</form>'
            );
            scope = $rootScope.$new();
            scope.data = { serverToServerEnabled: true, callbackURL: undefined };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid if server to server is enabled and callback URL field is empty', function() {
            scope.data.serverToServerEnabled = true;
            scope.data.callbackURL = undefined;
            scope.$digest();
            expect(form.callbackURL.$valid).toEqual(false);
        });

        it('should be invalid if the callback URL value does not start with \'http\' or \'https\'', function() {
            scope.data.serverToServerEnabled = true;
            scope.data.callbackURL = 'someurl.com';
            scope.$digest();
            expect(form.callbackURL.$valid).toEqual(false);
        });

        it('should be valid if the callback URL value starts with \'http\' or \'https\'', function() {
            scope.data.serverToServerEnabled = true;
            scope.data.callbackURL = 'http://someurl.com';
            scope.$digest();
            expect(form.callbackURL.$valid).toEqual(true);
        });

        it('should be valid if the callback URL is empty but server to server is not enabled', function() {
            scope.data.serverToServerEnabled = false;
            scope.data.callbackURL = undefined;
            scope.$digest();
            expect(form.callbackURL.$valid).toEqual(true);
        });
    });
});
