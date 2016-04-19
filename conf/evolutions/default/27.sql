# --- !Ups

ALTER TABLE ad_providers ADD COLUMN display_name VARCHAR(255) DEFAULT '' NOT NULL;

UPDATE ad_providers SET display_name = 'AdColony' WHERE name = 'AdColony';
UPDATE ad_providers SET display_name = 'AppLovin' WHERE name = 'AppLovin';
UPDATE ad_providers SET display_name = 'HyprMarketplace' WHERE name = 'HyprMarketplace';
UPDATE ad_providers SET display_name = 'Vungle' WHERE name = 'Vungle';
UPDATE ad_providers SET display_name = 'Unity Ads' WHERE name = 'Unity Ads';
UPDATE ad_providers SET name = 'UnityAds' WHERE display_name = 'Unity Ads';

# --- !Downs

ALTER TABLE ad_providers DROP COLUMN display_name;
