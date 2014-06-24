# Users schema
 
# --- !Ups
 
CREATE TABLE Distributor (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Property (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	distributor_id bigint(20) NOT NULL,
	name varchar(255) NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (distributor_id) REFERENCES Distributor(id)
);
 
# --- !Downs
 
DROP TABLE Property;
DROP TABLE Distributor;
