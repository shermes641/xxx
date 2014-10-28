
# --- !Ups

CREATE TABLE distributor_users (
  id bigserial,
  email varchar(255) NOT NULL,
  hashed_password varchar(255) NOT NULL,
  salt varchar(255) NOT NULL,
  primary key (id)
);

CREATE INDEX email_index on distributor_users(LOWER(email));

# --- !Downs

DROP TABLE distributor_users;



