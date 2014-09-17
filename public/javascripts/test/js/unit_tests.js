"use strict";

require.config( {
    baseUrl: '/assets/javascripts',
    paths: {
        QUnit: 'test/js/libs/qunit',
        tests: 'test/js/tests'
    },
    shim: {
       'QUnit': {
           exports: 'QUnit',
           init: function() {
               QUnit.config.autoload = false;
               QUnit.config.autostart = false;
           }
       } 
    }
} );

// require the unit tests.
require(
    [ 'QUnit', 'tests/analytics_test' ],
    function( QUnit, analyticsTest ) {

        // start QUnit.
        QUnit.load();
        QUnit.start();

        // run the tests.
        analyticsTest.run();

    }
);