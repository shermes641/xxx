/*
    Checks browser support from our list in https://wiki.jungroup.com/display/MED/Browser+and+Device+Support
    Saves dismiss state to only show once per session.
 */
mediationModule.controller( 'BrowserSupportController', [ '$scope',

    function( $scope ) {
        $scope.popupElement = $( '#browser_support' );
        $scope.supportedBrowsers = [ "chrome", "safari", "firefox" ];
        $scope.cookieString = "dismissed_browser_popup";

        // Returns browser type
        $scope.browserType = function() {
            return $.browser.name;
        };

        // Returns the current users OS
        $scope.osType = function() {
            return $.os.name;
        };

        // Returns true if the browser is supported, false otherwise.
        // Extra check for safari on supported on mac and not PC
        $scope.isBrowserSupported = function() {
            if( this.supportedBrowsers.indexOf( $scope.browserType() ) === -1 || ( $scope.browserType() === "safari" && $scope.osType() !== "mac" )) {
                return false
            } else {
                return true
            }
        };

        // Runs browser check and opens popup on completion
        $scope.checkBrowser = function() {
            if( !$scope.isBrowserSupported() || $scope.checkMobile() ){
                $scope.openPopup();
            }
        };

        // Taken from http://detectmobilebrowsers.com/
        $scope.checkMobile = function(useragent) {
            useragent = typeof useragent !== 'undefined' ? useragent : navigator.userAgent||navigator.vendor||window.opera;
            var check = false;
            (function(a,b){if(/Mobile|iP(hone|od|ad)|Android|BlackBerry|IEMobile|Kindle|NetFront|Silk-Accelerated|(hpw|web)OS|Fennec|Minimo|Opera M(obi|ini)|Blazer|Dolfin|Dolphin|Skyfire|Zune/.test(a))check = true})(useragent);
            return check;
        };

        // Used for development to clear cookie
        $scope.clearCookie = function() {
            document.cookie = "browser_support=; expires=' + expirationDate.toUTCString() + '; path=/";
        };

        // Opens popup
        $scope.openPopup = function() {
            if( document.cookie.indexOf( $scope.cookieString ) === -1 ){
                angluar.element(document.body).addClass('browser_not_supported');
                $( '#browser_support' ).css('display','block');
            }
        };

        // Closes popup and sets cookie
        $scope.dismissPopup = function() {
            angluar.element(document.body).removeClass('browser_not_supported');
            $scope.popupElement.hide();
            var expirationDate = new Date();
            var time = expirationDate.getTime();
            // 24 hours (1000 milliseconds * 60 seconds * 60 minutes * 24 hours)
            time += 24 * 60 * 60 * 1000;
            expirationDate.setTime(time);
            document.cookie = 'browser_support=' + $scope.cookieString + '; expires=' + expirationDate.toUTCString() + '; path=/';
        };

        $scope.checkBrowser();
    } ]
);

/*
 * jQuery BrowserTest Plugin
 * Version 2.4
 * URL: http://jquery.thewikies.com/browser
 * Description: jQuery Browser Plugin extends browser detection capabilities and
 * can assign browser selectors to CSS classes.
 * Author: Nate Cavanaugh, Minhchau Dang, Jonathan Neal, & Gregory Waxman
 * Updated By: Steven Bower for use with jReject plugin
 * Copyright: Copyright (c) 2008 Jonathan Neal under dual MIT/GPL license.
 */
(function($){$.browserTest=function(a,z){var u='unknown',x='X',m=function(r,h){for(var i=0;i<h.length;i=i+1){r=r.replace(h[i][0],h[i][1]);}return r;},c=function(i,a,b,c){var r={name:m((a.exec(i)||[u,u])[1],b)};r[r.name]=true;r.version=(c.exec(i)||[x,x,x,x])[3];if(r.name.match(/safari/)&&r.version>400){r.version='2.0';}if(r.name==='presto'){r.version=($.browser.version>9.27)?'futhark':'linear_b';}r.versionNumber=parseFloat(r.version,10)||0;r.versionX=(r.version!==x)?(r.version+'').substr(0,1):x;r.className=r.name+r.versionX;return r;};a=(a.match(/Opera|Navigator|Minefield|KHTML|Chrome/)?m(a,[[/(Firefox|MSIE|KHTML,\slike\sGecko|Konqueror)/,''],['Chrome Safari','Chrome'],['KHTML','Konqueror'],['Minefield','Firefox'],['Navigator','Netscape']]):a).toLowerCase();$.browser=$.extend((!z)?$.browser:{},c(a,/(camino|chrome|firefox|netscape|konqueror|lynx|msie|opera|safari)/,[],/(camino|chrome|firefox|netscape|netscape6|opera|version|konqueror|lynx|msie|safari)(\/|\s)([a-z0-9\.\+]*?)(\;|dev|rel|\s|$)/));$.layout=c(a,/(gecko|konqueror|msie|opera|webkit)/,[['konqueror','khtml'],['msie','trident'],['opera','presto']],/(applewebkit|rv|konqueror|msie)(\:|\/|\s)([a-z0-9\.]*?)(\;|\)|\s)/);$.os={name:(/(win|mac|linux|sunos|solaris|iphone)/.exec(navigator.platform.toLowerCase())||[u])[0].replace('sunos','solaris')};if(!z){$('html').addClass([$.os.name,$.browser.name,$.browser.className,$.layout.name,$.layout.className].join(' '));}};$.browserTest(navigator.userAgent);})(jQuery);