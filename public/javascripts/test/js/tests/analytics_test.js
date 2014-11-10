"use strict";

define([], function() {
        
        var run = function() {
            var analytics = new Analytics();

            QUnit.module( "Analytics Test" );
            test( 'Keen should be initialized correctly', function() {
                notEqual( typeof Keen, "undefined", 'Keen should be defined' );
            } );

            test( 'Filters have been created correctly', function() {
                var filters = analytics.buildFilters( [ '1' ], [ "United States" ], [ 10 ] );
                var appId = _.find( filters, function( filter ) {
                    return filter.property_name === "app_id";
                } );

                equal( appId.property_value, 1, 'App ID Should be 1' );

                var country = _.find( filters, function( filter ) {
                    return filter.property_name === "ip_geo_info.country";
                } );

                equal( country.property_value, "United States", 'Country Should be United States' );

                var ad_provider = _.find( filters, function( filter ) {
                    return filter.property_name === "ad_provider";
                } );

                equal( ad_provider.property_value, 10, 'Ad Provider Should be 10' );
            } );

            test( 'Dates should be validated correctly', function() {
                var goodDate = new Date();
                ok( analytics.isValidDate( goodDate ), 'Date is valid' );

                var badDate = new Date('Cannot parse this');
                ok( !analytics.isValidDate( badDate ), 'Date is invalid' );
            } );
        };
        
        return { run: run }
    }
);