describe('HeaderController', function() {
    beforeEach(module('MediationModule'));

    describe('sharedIDs', function(){
        it('should initialize app ID to be 0', function() {
            inject(function(sharedIDs) {
                expect(sharedIDs.appID()).toEqual(0);
            });
        });

        it('should set the app ID correctly', function() {
            inject(function(sharedIDs) {
                var appID = 12345;
                sharedIDs.setAppID(appID);
                expect(sharedIDs.appID()).toEqual(appID);
            });
        });

        it('should initialize distributor ID to be 0', function() {
            inject(function(sharedIDs) {
                expect(sharedIDs.distributorID()).toEqual(0);
            });
        });

        it('should set the distributor ID correctly', function(){
            inject(function(sharedIDs) {
                var distributorID = 54321;
                sharedIDs.setDistributorID(distributorID);
                expect(sharedIDs.distributorID()).toEqual(distributorID);
            });
        });
    });
});
