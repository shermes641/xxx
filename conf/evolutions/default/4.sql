
# --- !Ups

CREATE TABLE DistributorUser (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	email varchar(255) NOT NULL,
	hashed_password varchar(255) NOT NULL,
	salt varchar(255) NOT NULL,
	primary key (id)
);

CREATE INDEX email_index on DistributorUser (email)

# --- !Downs

DROP TABLE DistributorUser;



