# --- !Ups

CREATE TABLE completions (
    id bigserial,
    app_token varchar(255) NOT NULL,
    ad_provider_name varchar(255) NOT NULL,
    transaction_id varchar(255) NOT NULL,
    offer_profit numeric,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE completions;
