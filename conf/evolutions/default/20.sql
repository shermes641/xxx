# --- !Ups

ALTER TABLE apps ALTER COLUMN callback_url TYPE VARCHAR(2048);

# --- !Downs

ALTER TABLE apps ALTER COLUMN callback_url TYPE VARCHAR(255);
