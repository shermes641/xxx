# --- !Ups

ALTER TABLE waterfalls ADD COLUMN paused BOOL NOT NULL DEFAULT false;

# --- !Downs

ALTER TABLE waterfalls DROP COLUMN IF EXISTS paused;
