# --- !Ups

ALTER TABLE ad_providers ADD COLUMN callback_url_description VARCHAR(512) DEFAULT '' NOT NULL;

# --- !Downs

ALTER TABLE ad_providers DROP COLUMN callback_url_description;
