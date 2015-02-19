# Users schema
 
# --- !Ups

CREATE EXTENSION "uuid-ossp";

/**
  This function will return a positive, unique 32-bit number per the documentation on the PostgerSQL wiki: https://wiki.postgresql.org/wiki/Pseudo_encrypt
  The reason for modifying the original function as it exists on PostreSQL's wiki is to prevent JavaScript's Number overflow issue for numbers that are larger than +/- 9007199254740992.
  It is important that the input of this function be less than 2^30 to avoid ID collision.
 **/
CREATE OR REPLACE FUNCTION pseudo_encrypt(VALUE int) returns bigint AS $$
DECLARE
l1 int;;
l2 int;;
r1 int;;
r2 int;;
i int:=0;;
BEGIN
 l1:= (VALUE >> 15) & 32767;;
 r1:= VALUE & 32767;;
 WHILE i < 3 LOOP
   l2 := r1;;
   r2 := l1 # ((((1366.0 * r1 + 150889) % 714025) / 714025.0) * 32767)::int;;
   l1 := l2;;
   r1 := r2;;
   i := i + 1;;
 END LOOP;;
 RETURN ((l1::bigint << 15) + r1);;
END;;
$$ LANGUAGE plpgsql strict immutable;;

CREATE SEQUENCE distributors_id_seq;

CREATE TABLE distributors (
    id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('distributors_id_seq')::int),
    name varchar(255) NOT NULL
);

ALTER SEQUENCE distributors_id_seq OWNED BY distributors.id;

CREATE SEQUENCE apps_id_seq;

CREATE TABLE apps (
  id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('apps_id_seq')::int),
  token varchar(255) NOT NULL UNIQUE,
  active BOOL NOT NULL DEFAULT TRUE,
  distributor_id bigint NOT NULL,
  name varchar(255) NOT NULL,
  callback_url varchar(255),
  server_to_server_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  app_config_refresh_interval bigint NOT NULL DEFAULT 0,
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
