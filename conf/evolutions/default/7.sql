# --- !Ups

ALTER TABLE distributor_users ADD COLUMN distributor_id bigint;
ALTER TABLE distributor_users ADD FOREIGN KEY (distributor_id) REFERENCES distributors(id);

CREATE INDEX distributor_id_index on distributor_users(distributor_id);

# --- !Downs

ALTER TABLE distributor_users DROP COLUMN distributor_id;
