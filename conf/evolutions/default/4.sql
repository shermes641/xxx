
# --- !Ups

CREATE SEQUENCE distributor_users_id_seq;

CREATE TABLE distributor_users (
  id bigint PRIMARY KEY DEFAULT pseudo_encrypt(nextval('distributor_users_id_seq')::int),
  email varchar(255) NOT NULL,
  hashed_password varchar(255) NOT NULL,
  salt varchar(255) NOT NULL
);

ALTER SEQUENCE distributor_users_id_seq OWNED BY distributor_users.id;

CREATE INDEX email_index on distributor_users(LOWER(email));

# --- !Downs

DROP TABLE distributor_users;
