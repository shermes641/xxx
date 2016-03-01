describe('FlashMessageSpec', function() {
    beforeEach(module('MediationModule'));

    describe('waterfallPage', function() {
        beforeEach(inject(function($rootScope, $controller) {
            scope = $rootScope.$new();
            testCont = $controller('WaterfallController', {$scope: scope});
        }));
        var dummyMsg = {status: "success", message: "123ABC"};
        var msg = [{
                status: "error",
                message: "There was a problem creating your app",
                priority: "LOW"
            },
            {
                status: "success",
                message: "There was no severe problem creating your app",
                priority: "HIGH"
            },
            {
                status: "error",
                message: "There was a severe problem creating your app",
                priority: "HIGH"
            },
            {
                status: "success",
                message: "There was no problem creating your app",
                priority: "LOW"
            }];

        it('should not insert duplicate error messages', function() {
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);

            expect(scope.flashMessage.text()).toEqual(msg[0].message);
            expect(scope.flashMessage.queue().length).toEqual(0);
        });

        it('should not insert duplicate success messages', function() {
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);

            expect(scope.flashMessage.text()).toEqual(msg[1].message);
            expect(scope.flashMessage.queue().length).toEqual(0);
        });

        it('should insert duplicate success message in priority order', function() {
            scope.flashMessage.add(dummyMsg);
            scope.flashMessage.add(msg[3]);
            scope.flashMessage.add(msg[3]);
            scope.flashMessage.add(msg[3]);
            scope.flashMessage.add(msg[3]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[1]);

            expect(scope.flashMessage.text()).toEqual(dummyMsg.message);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[1].message);
            expect(scope.flashMessage.queue().length).toEqual(4);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[3].message);
            expect(scope.flashMessage.queue().length).toEqual(0);
        });

        it('should insert duplicate error messages in priority order', function() {
            scope.flashMessage.add(dummyMsg);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[2]);
            scope.flashMessage.add(msg[2]);
            scope.flashMessage.add(msg[2]);
            scope.flashMessage.add(msg[2]);

            expect(scope.flashMessage.text()).toEqual(dummyMsg.message);
            expect(scope.flashMessage.queue().length).toEqual(8);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[2].message);
            expect(scope.flashMessage.queue().length).toEqual(4);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[0].message);
            expect(scope.flashMessage.queue().length).toEqual(0);
        });

        it('should insert messages in priority order', function() {
            scope.flashMessage.add(dummyMsg);
            scope.flashMessage.add(msg[0]);
            scope.flashMessage.add(msg[1]);
            scope.flashMessage.add(msg[2]);
            scope.flashMessage.add(msg[3]);

            expect(scope.flashMessage.text()).toEqual(dummyMsg.message);
            expect(scope.flashMessage.queue().length).toEqual(4);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[2].message);
            expect(scope.flashMessage.queue().length).toEqual(3);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[1].message);
            expect(scope.flashMessage.queue().length).toEqual(2);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[0].message);
            expect(scope.flashMessage.queue().length).toEqual(1);
            scope.flashMessage.forceDisplay();
            expect(scope.flashMessage.text()).toEqual(msg[3].message);
            expect(scope.flashMessage.queue().length).toEqual(0);
        });
    });
});
