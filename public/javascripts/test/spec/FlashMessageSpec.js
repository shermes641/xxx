describe('FlashMessageSpec', function () {
    beforeEach(module('MediationModule'));

    describe('waterfallPage', function () {
        beforeEach(inject(function ($rootScope, $controller) {
            scope = $rootScope.$new();
            testCont = $controller('WaterfallController', {$scope: scope});
        }));

        var LoTimeOut = 3000;
        var HiTimeOut = 5000;
        var msgIdx = {
            ErrorLo : 0,
            SuccessHi : 1,
            ErrorHi : 2,
            SuccessLo : 3,
            ErrorUndefined : 4,
            SuccessUndefined : 5,
            DummyUndefined : 6,
            DummyHi : 7,
            DummyLo : 8
        };
        var msg = [{status: "error", message: "There was a problem creating your app", priority: "LOW"},
            {status: "success", message: "There was no severe problem creating your app", priority: "HIGH"},
            {status: "error", message: "There was a severe problem creating your app", priority: "HIGH"},
            {status: "success", message: "There was no problem creating your app", priority: "LOW"},
            {status: "error", message: "ERORR There was no problem creating your app undefined priority"},
            {status: "success", message: "SUCCESS There was no problem creating your app undefined priority"},
            {status: "success", message: "123ABC"},
            {status: "success", message: "123ABC3421", priority: "HIGH"},
            {status: "success", message: "zxces123ABC", priority: "LOW"}];

        function checkMsg(msg, cnt, to) {
            expect(scope.flashMessage.text()).toEqual(msg.message);
            expect(scope.flashMessage.queue().length).toEqual(cnt);
            expect(scope.flashMessage.getTimeOut()).toEqual(to);
        }

        it('should not insert duplicate error messages', function () {
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);

            checkMsg(msg[msgIdx.ErrorLo], 0, LoTimeOut);
        });

        it('should not insert duplicate success messages', function () {
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);

            checkMsg(msg[msgIdx.SuccessHi], 0, HiTimeOut);
        });

        it('should insert duplicate success message in priority order', function () {
            scope.flashMessage.add(msg[msgIdx.DummyUndefined]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);

            checkMsg(msg[msgIdx.SuccessHi], 4, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.SuccessLo], 0, LoTimeOut);
        });

        it('should insert duplicate error messages in priority order', function () {
            scope.flashMessage.add(msg[msgIdx.DummyUndefined]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);

            checkMsg(msg[msgIdx.ErrorHi], 4, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.ErrorLo], 0, LoTimeOut);
        });

        it('should insert messages in priority order', function () {
            scope.flashMessage.add(msg[msgIdx.DummyUndefined]);
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.DummyUndefined]);
            scope.flashMessage.add(msg[msgIdx.DummyLo]);
            scope.flashMessage.add(msg[msgIdx.DummyHi]);

            checkMsg(msg[msgIdx.SuccessHi], 6, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.DummyHi], 5, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.ErrorHi], 4, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.ErrorLo], 3, LoTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.SuccessLo], 2, LoTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.DummyUndefined], 1, LoTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.DummyLo], 0, LoTimeOut);
        });

        it('high priority message should replace low priority message', function () {
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            scope.flashMessage.add(msg[msgIdx.ErrorHi]);
            scope.flashMessage.add(msg[msgIdx.SuccessLo]);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);

            checkMsg(msg[msgIdx.ErrorHi], 2, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.SuccessHi], 1, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.SuccessLo], 0, LoTimeOut);
        });

        it('high priority message should not replace high priority message immediately', function () {
            scope.flashMessage.add(msg[msgIdx.DummyHi]);
            checkMsg(msg[msgIdx.DummyHi], 0, HiTimeOut);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            checkMsg(msg[msgIdx.DummyHi], 1, HiTimeOut);
            scope.flashMessage.forceDisplay();
            checkMsg(msg[msgIdx.SuccessHi], 0, HiTimeOut);
        });

        it('high priority message should replace low priority message immediately', function () {
            scope.flashMessage.add(msg[msgIdx.ErrorLo]);
            checkMsg(msg[msgIdx.ErrorLo], 0, LoTimeOut);
            scope.flashMessage.add(msg[msgIdx.SuccessHi]);
            checkMsg(msg[msgIdx.SuccessHi], 0, HiTimeOut);
        });
    });
});
