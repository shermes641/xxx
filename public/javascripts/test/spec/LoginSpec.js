describe('LoginController', function() {
    beforeEach(module('MediationModule'));

    describe('LoginController', function() {
        var scope, testCont;

        beforeEach(inject(function($rootScope, $controller) {
            scope = $rootScope.$new();
            testCont = $controller('LoginController', {$scope: scope});
        }));

        it('should be defined', function() {
            expect(testCont).toBeDefined();
        });

        it('should be initalized correctly', function() {
            expect(scope.waitForAuth).toEqual(false);
            expect(scope.errors).toEqual({});
        });
    });
});
