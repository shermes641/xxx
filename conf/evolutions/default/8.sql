# --- !Ups

CREATE TABLE virtual_currencies (
    id bigserial,
    name varchar(255) NOT NULL,
    exchange_rate bigint NOT NULL,
    reward_min bigint,
    reward_max bigint,
    round_up BOOL NOT NULL DEFAULT FALSE,
    app_id bigint,
    FOREIGN KEY (app_id) REFERENCES apps(id),
    PRIMARY KEY (id)
);

CREATE INDEX virtual_currencies_app_id_index on virtual_currencies(app_id);

# --- !Downs

DROP TABLE virtual_currencies;
