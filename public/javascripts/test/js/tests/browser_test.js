"use strict";

define([], function() {
        
        var run = function() {
            $( '#qunit-fixture' ).html('<div id="browser_support" style="display: none;">The browser you are using is not actively supported.<div id="dismiss_browser">Dismiss</div></div>');
            var browserSupport = new BrowserSupport();


            QUnit.module( "Browser Test" );

            test( 'Test browser checks', function() {
                var samplePassingBrowsers = [
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"
                ];

                var sampleFailingBrowsers = [
                    "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)",
                    "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)",
                    "Opera/9.52 (Windows NT 5.1; U; en)"
                ];

                _.each( samplePassingBrowsers, function( useragent ) {
                    $.browserTest( useragent );
                    equal( browserSupport.isBrowserSupported(), true, useragent + 'Browser Should pass' );
                } );

                _.each( sampleFailingBrowsers, function( useragent ) {
                    $.browserTest( useragent );
                    equal( browserSupport.isBrowserSupported(), false, useragent + ' Should fail' );
                } );
            } );

            test( 'Test popup functionality', function() {
                equal( browserSupport.popupElement.css( 'display' ), "none", "browser popup should be hidden at start." );

                equal( document.cookie.indexOf( browserSupport.cookieString ), -1, "Cookie should not be set" );

                browserSupport.openPopup();
                equal( browserSupport.popupElement.css( 'display' ), "block", "browser popup should be shown." );

                browserSupport.dismissPopup();
                notEqual( document.cookie.indexOf( browserSupport.cookieString ), -1, "Cookie should be set" );
                equal( browserSupport.popupElement.css( 'display' ), "none", "browser popup should hidden" );

                browserSupport.openPopup();
                equal( browserSupport.popupElement.css( 'display' ), "none", "browser support should not be shown after cookie is set." );


                browserSupport.clearCookie();
                equal( document.cookie.indexOf( browserSupport.cookieString ), -1, "Cookie should not be set after clear" );
            } );
        };
        
        return { run: run }
    }
);