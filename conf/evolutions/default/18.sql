# --- !Ups

ALTER TABLE ad_providers ADD COLUMN sdk_blacklist_regex VARCHAR(255) NOT NULL DEFAULT '.^';

# --- !Downs

ALTER TABLE ad_providers DROP COLUMN IF EXISTS sdk_blacklist_regex;
