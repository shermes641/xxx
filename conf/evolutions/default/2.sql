# Users schema
 
# --- !Ups
 
CREATE TABLE ad_providers (
  id bigserial,
  name varchar(255) NOT NULL,
  configuration_data json NOT NULL DEFAULT to_json('{}'::JSON),
  callback_url_format varchar(2083),
  configurable BOOL NOT NULL DEFAULT TRUE,
  default_ecpm float,
  PRIMARY KEY (id)
);

CREATE SEQUENCE waterfalls_id_seq;

CREATE TABLE waterfalls (
  id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('waterfalls_id_seq')),
  app_id bigint NOT NULL,
  name varchar(255) NOT NULL,
  token varchar(255) NOT NULL,
  optimized_order BOOL NOT NULL DEFAULT TRUE,
  test_mode BOOL NOT NULL DEFAULT TRUE,
  FOREIGN KEY (app_id) REFERENCES apps(id)
);

ALTER SEQUENCE waterfalls_id_seq OWNED BY waterfalls.id;

CREATE INDEX app_id_index on waterfalls(app_id);
CREATE INDEX token_index on waterfalls(token);
 
# --- !Downs
 
DROP TABLE waterfalls;
DROP SEQUENCE waterfalls_id_seq;

DROP TABLE ad_providers;
