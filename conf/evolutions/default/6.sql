
# --- !Ups

ALTER TABLE distributor_users DROP COLUMN salt;

# --- !Downs

ALTER TABLE distributor_users ADD COLUMN salt varchar(255) NOT NULL,



