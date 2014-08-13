# Users schema
 
# --- !Ups
 
CREATE TABLE distributors (
    id bigserial,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE apps (
  id bigserial,
  active BOOL NOT NULL DEFAULT TRUE,
  distributor_id bigint NOT NULL,
  name varchar(255) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (distributor_id) REFERENCES distributors(id)
);

CREATE INDEX distributor_id_index on apps(distributor_id);
 
# --- !Downs
 
DROP TABLE apps;
DROP TABLE distributors;
