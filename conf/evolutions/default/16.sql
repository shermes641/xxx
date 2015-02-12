# --- !Ups

ALTER TABLE completions ADD COLUMN ad_provider_request json NOT NULL DEFAULT to_json('{}'::JSON);
CREATE INDEX completions_app_token_index ON completions(app_token);
CREATE INDEX completions_ad_provider_name_index ON completions(ad_provider_name);

# --- !Downs

ALTER TABLE completions DROP COLUMN IF EXISTS ad_provider_request;
DROP INDEX IF EXISTS completions_app_token_index;
DROP INDEX IF EXISTS completions_ad_provider_name_index;
