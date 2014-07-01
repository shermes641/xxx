
# --- !Ups

ALTER TABLE DistributorUser DROP COLUMN salt;

# --- !Downs

ALTER TABLE DistributorUser ADD COLUMN salt varchar(255) NOT NULL,



