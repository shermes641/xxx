# --- !Ups

ALTER TABLE distributor_users ADD COLUMN active BOOL NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE distributor_users DROP COLUMN active;