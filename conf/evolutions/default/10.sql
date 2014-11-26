# --- !Ups

CREATE TABLE app_configs (
    id bigserial,
    generation_number bigint NOT NULL,
    app_id bigint NOT NULL,
    app_token VARCHAR(255) NOT NULL,
    configuration json NOT NULL DEFAULT to_json('{}'::JSON),
    FOREIGN KEY (app_id) REFERENCES apps(id),
    PRIMARY KEY (id)
);

CREATE INDEX app_configs_app_id_index on app_configs(app_id);
CREATE INDEX app_configs_app_token_index on app_configs(app_token);
CREATE UNIQUE INDEX app_token_generation_number_index on app_configs(app_token, generation_number);

# --- !Downs

DROP TABLE app_configs;
