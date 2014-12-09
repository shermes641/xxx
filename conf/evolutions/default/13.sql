# --- !Ups

ALTER TABLE waterfall_ad_providers ADD COLUMN pending BOOL DEFAULT FALSE;
ALTER TABLE distributor_users DROP COLUMN active;
ALTER TABLE distributors DROP COLUMN hypr_marketplace_id;

# --- !Downs

ALTER TABLE waterfall_ad_providers DROP COLUMN pending;
ALTER TABLE distributor_users ADD COLUMN active BOOL NOT NULL DEFAULT FALSE;
ALTER TABLE distributors ADD COLUMN hypr_marketplace_id int;
