# --- !Ups

ALTER TABLE DistributorUser ADD COLUMN distributor_id bigint(20) NOT NULL;
ALTER TABLE DistributorUser ADD FOREIGN KEY (distributor_id) REFERENCES Distributor(id);

# --- !Downs

ALTER TABLE DistributorUser DROP COLUMN distributor_id;
