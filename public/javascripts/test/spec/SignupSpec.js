describe('SignupController', function() {
    beforeEach(module('MediationModule'));


    describe('SignupController', function() {
        var scope, testCont, compile;

        beforeEach(inject(function($rootScope, $controller, $compile) {
            scope = $rootScope.$new();
            testCont = $controller('SignupController', {$scope: scope});
            compile = $compile;
        }));

        it('should be defined', function() {
            expect(testCont).toBeDefined();
        });

        it('should be initalized correctly', function() {
            expect(scope.waitForAuth).toEqual(false);
            expect(scope.termsTemplate).toEqual('assets/templates/distributor_users/terms.html');
            expect(scope.showTerms).toEqual(false);
            expect(scope.errors).toEqual({});
        });

        it('should toggle the value of showTerms when toggleTerms is called', function() {
            scope.toggleTerms();
            expect(scope.showTerms).toEqual(true);
            scope.toggleTerms();
            expect(scope.showTerms).toEqual(false);
        });
    });

    describe('passwordConfirmation', function() {
        var scope, form;

        beforeEach(inject(function($rootScope, $compile) {
            var element = angular.element(
                '<form name="form">' +
                '<input name="password" ng-model="data.password">' +
                '<input name="confirmation" password-confirmation="{{data.password}}" ng-model="data.confirmation">' +
                '</form>'
            );

            scope = $rootScope.$new();
            scope.data = { password: null, confirmation: null };
            $compile(element)(scope);
            form = scope.form;
        }));

        it('should be invalid when the password does not match the password confirmation', function() {
            scope.data.password = "password";
            scope.data.confirmation = "differentpassword";
            scope.$digest();
            expect(form.confirmation.$valid).toEqual(false);
        });

        it('should be valid when the password matches the password confirmation', function() {
            var password = "password";
            scope.data.password = password;
            scope.data.confirmation = password;
            scope.$digest();
            expect(form.confirmation.$valid).toEqual(true);
        });

        it('should be valid when the password does not meet the minimum length', function() {
            var password = "test";
            scope.data.password = password;
            scope.data.confirmation = password;
            scope.$digest();
            expect(form.confirmation.$valid).toEqual(true);
        });
    });
});
