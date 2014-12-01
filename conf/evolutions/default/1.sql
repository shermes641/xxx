# Users schema
 
# --- !Ups

CREATE EXTENSION "uuid-ossp";

CREATE OR REPLACE FUNCTION pseudo_encrypt(VALUE bigint) returns bigint AS $$
DECLARE
l1 bigint;;
l2 bigint;;
r1 bigint;;
r2 bigint;;
i int:=0;;
BEGIN
    l1:= (VALUE >> 32) & 4294967295::bigint;;
    r1:= VALUE & 4294967295;;
    WHILE i < 3 LOOP
        l2 := r1;;
        r2 := l1 # ((((1366.0 * r1 + 150889) % 714025) / 714025.0) * 32767*32767)::int;;
        l1 := l2;;
        r1 := r2;;
        i := i + 1;;
    END LOOP;;
RETURN ((l1::bigint << 32) + r1);;
END;;
$$ LANGUAGE plpgsql strict immutable;

CREATE SEQUENCE distributors_id_seq;

CREATE TABLE distributors (
    id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('distributors_id_seq')),
    name varchar(255) NOT NULL
);

ALTER SEQUENCE distributors_id_seq OWNED BY distributors.id;

CREATE SEQUENCE apps_id_seq;

CREATE TABLE apps (
  id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('apps_id_seq')),
  token varchar(255) NOT NULL UNIQUE,
  active BOOL NOT NULL DEFAULT TRUE,
  distributor_id bigint NOT NULL,
  name varchar(255) NOT NULL,
  callback_url varchar(255),
  server_to_server_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (distributor_id) REFERENCES distributors(id)
);

ALTER SEQUENCE apps_id_seq OWNED BY apps.id;

CREATE INDEX distributor_id_index on apps(distributor_id);
 
# --- !Downs

DROP EXTENSION IF EXISTS "uuid-ossp";

DROP TABLE apps;
DROP SEQUENCE apps_id_seq;

DROP TABLE distributors;
DROP SEQUENCE distributors_id_seq;
