# --- !Ups

CREATE TABLE waterfall_generations (
    id bigserial,
    generation_number bigint NOT NULL DEFAULT 0,
    waterfall_id bigint NOT NULL,
    waterfall_token VARCHAR(255) NOT NULL,
    configuration json NOT NULL DEFAULT to_json('{}'::JSON),
    FOREIGN KEY (waterfall_id) REFERENCES waterfalls(id),
    PRIMARY KEY (id)
);

CREATE INDEX waterfall_generations_waterfall_id_index on waterfall_generations(waterfall_id);
CREATE INDEX waterfall_generations_waterfall_token_index on waterfall_generations(waterfall_token);

# --- !Downs

DROP TABLE waterfall_generations;
