# --- !Ups

-- Add hmac_secret column to apps table and populate the column for existing apps

-- some extra safety here
-- the downside is if the hmac_secret column exists, all existing apps will get a new hmac secret
ALTER TABLE apps DROP COLUMN IF EXISTS hmac_secret;

ALTER TABLE apps ADD COLUMN hmac_secret VARCHAR(255) DEFAULT uuid_generate_v4() NOT NULL;

# --- !Downs

ALTER TABLE apps DROP COLUMN hmac_secret;
