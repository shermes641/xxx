# Users schema
 
# --- !Ups
 
CREATE TABLE ad_providers (
  id bigserial,
  name varchar(255) NOT NULL,
  configuration_data json NOT NULL DEFAULT to_json('{}'::JSON),
  configurable BOOL NOT NULL DEFAULT TRUE,
  default_ecpm float,
  PRIMARY KEY (id)
);

CREATE TABLE waterfalls (
  id bigserial,
  app_id bigint NOT NULL,
  name varchar(255) NOT NULL,
  token varchar(255) NOT NULL,
  optimized_order BOOL NOT NULL DEFAULT TRUE,
  test_mode BOOL NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  FOREIGN KEY (app_id) REFERENCES apps(id)
);

CREATE INDEX app_id_index on waterfalls(app_id);
CREATE INDEX token_index on waterfalls(token);
 
# --- !Downs
 
DROP TABLE waterfalls;
DROP TABLE ad_providers;
