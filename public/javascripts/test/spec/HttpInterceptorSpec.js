describe('HttpInterceptorSpec', function() {
    beforeEach(module('MediationModule'));


    // Factory of interest is called MyFactory
    describe('httpErrorInterceptor', function() {
        var factory = null;

        it('Should define methods', function() {
            inject(function(httpErrorInterceptor) {
                factory = httpErrorInterceptor;
            });

            expect(factory.responseError).toBeDefined()
            expect(factory.responseError).toEqual(jasmine.any(Function))
        });

        it('Should add message to flashMessage on 503', function() {
            module(function ($provide) {
                $provide.value('flashMessage', {
                    add: function(data){
                        expect(data.message).toEqual("We are currently down for maintenance.  Please try again later.");
                    }
                });
            })

            inject(function(httpErrorInterceptor) {
                factory = httpErrorInterceptor;
            });

            factory.responseError({status: 503});
        });

        it('Should add message to flashMessage on no connection', function() {
            module(function ($provide) {
                $provide.value('flashMessage', {
                    add: function(data){
                        expect(data.message).toEqual("There was a problem with the request.  Please try again later.");
                    }
                });
            })

            inject(function(httpErrorInterceptor) {
                factory = httpErrorInterceptor;
            });

            factory.responseError({status: 0});
        });
    });

});