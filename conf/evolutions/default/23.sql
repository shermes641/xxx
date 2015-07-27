# --- !Ups

UPDATE waterfall_ad_providers SET pending = true WHERE ad_provider_id = 2 AND pending = false AND waterfall_ad_providers.active = false AND configuration_data->'requiredParams'->>'distributorID' = '';

# --- !Downs
