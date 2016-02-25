describe('HttpInterceptorSpec', function() {
    var factory, maintenanceMessage, lostConnectionMessage, errorMessage, injectInterceptor, validateMessage;

    beforeEach(module('MediationModule'));

    describe('httpErrorInterceptor', function() {
        maintenanceMessage = "We are currently down for maintenance.  Please try again later.";
        lostConnectionMessage = "There was a problem with the request.  Please try again later.";
        errorMessage = lostConnectionMessage;

        injectInterceptor = function() {
            inject(function(httpErrorInterceptor) {
                factory = httpErrorInterceptor;
            });
        };

        validateMessage = function(expectedMessage) {
            module(function ($provide) {
                $provide.value('flashMessage', {
                    add: function(data){
                        expect(data.message).toEqual(expectedMessage);
                    }
                });
            });
        };

        it('should define methods', function() {
            injectInterceptor();

            expect(factory.responseError).toBeDefined();
            expect(factory.responseError).toEqual(jasmine.any(Function))
        });

        it('should add message to flashMessage on a 503 response', function() {
            validateMessage(maintenanceMessage);
            injectInterceptor();
            factory.responseError({status: 503});
        });

        it('should add message to flashMessage on no connection', function() {
            validateMessage(lostConnectionMessage);
            injectInterceptor();
            factory.responseError({status: 0});
        });

        it('should add message to flashMessage on a 500 response', function() {
            validateMessage(errorMessage);
            injectInterceptor();
            factory.responseError({status: 500});
        });

        it('should not add message to flashMessage on responses with status codes other than 0 and 5XX', function() {
            var addFlashMessageSpy = jasmine.createSpy();
            module(function ($provide) {
                $provide.value('flashMessage', {
                    add: addFlashMessageSpy
                });
            });
            injectInterceptor();
            [100, 200, 300, 400].map(function(statusCode) {
                factory.responseError({status: statusCode})
            });
            expect(addFlashMessageSpy).not.toHaveBeenCalled();
        });
    });
});