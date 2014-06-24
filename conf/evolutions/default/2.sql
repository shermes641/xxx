# Users schema
 
# --- !Ups
 
CREATE TABLE AdProvider (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Waterfall (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	property_id bigint(20) NOT NULL,
	name varchar(255),
	PRIMARY KEY (id),
	FOREIGN KEY (property_id) REFERENCES Property(id)
);
 
# --- !Downs
 
DROP TABLE Waterfall;
DROP TABLE AdProvider;
