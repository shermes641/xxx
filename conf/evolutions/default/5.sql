
# --- !Ups

ALTER TABLE DistributorUser ADD UNIQUE (email)

# --- !Downs

ALTER TABLE DistributorUser DROP UNIQUE (email)



