
describe('BrowserSupportController', function() {
    beforeEach(module('MediationModule'));

    describe('BrowserSupportController', function(){
        var scope, testCont;

        beforeEach(inject(function($rootScope, $controller) {
            scope = $rootScope.$new();
            testCont = $controller('BrowserSupportController', {$scope: scope});
        }));

        it('should correctly fail bad browsers', function(){
            var sampleFailingBrowsers = [
                "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)",
                "Opera/9.52 (Windows NT 5.1; U; en)"
            ];

            _.each(sampleFailingBrowsers, function(useragent) {
                $.browserTest(useragent);
                expect(scope.isBrowserSupported()).toEqual(false);
            });
        });


        it('should correctly pass good browsers', function(){
            var samplePassingBrowsers = [
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"
            ];

            _.each(samplePassingBrowsers, function(useragent) {
                $.browserTest(useragent);
                expect(scope.isBrowserSupported()).toEqual(true);
            });
        });

        it('should correctly identify mobile browsers', function(){
            var sampleMobileBrowsers = [
                "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19",
                "Mozilla/5.0(iPad; U; CPU iPhone OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B314 Safari/531.21.10",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"
            ];

            _.each(sampleMobileBrowsers, function(useragent) {
                expect(scope.checkMobile(sampleMobileBrowsers)).toEqual(true);
            });

            var sampleNonMobileBrowsers = [
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"
            ];

            _.each(sampleNonMobileBrowsers, function(useragent) {
                expect(scope.checkMobile(sampleNonMobileBrowsers)).toEqual(false);
            });
        });

        it('should set cookie correctly.', function() {
            $.browserTest( "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36" );

            scope.clearCookie();
            // Browser popup should be hidden at start
            expect(document.cookie.indexOf(scope.cookieString)).toEqual(-1);

            scope.openPopup();
            //expect(scope.popupElement.css('display')).toEqual("block");

            scope.dismissPopup();
            expect(document.cookie.indexOf(scope.cookieString)).not.toEqual(-1);

            scope.clearCookie();
            expect(document.cookie.indexOf(scope.cookieString)).toEqual(-1);
        } );
    });
});