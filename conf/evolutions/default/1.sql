# Users schema
 
# --- !Ups
 
CREATE TABLE Distributor (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE App (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	active BOOL NOT NULL DEFAULT TRUE,
	distributor_id bigint(20) NOT NULL,
	name varchar(255) NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (distributor_id) REFERENCES Distributor(id)
);
 
# --- !Downs
 
DROP TABLE App;
DROP TABLE Distributor;
