
# --- !Ups

CREATE TABLE WaterfallAdProvider (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	waterfall_id bigint(20) NOT NULL,
	ad_provider_id bigint(20) NOT NULL,
	waterfall_order int,
	cpm float,
	active bit(1),
	fill_rate float,
	PRIMARY KEY (id),
	FOREIGN KEY (waterfall_id) REFERENCES Waterfall(id),
	FOREIGN KEY (ad_provider_id) REFERENCES AdProvider(id)
);
 
# --- !Downs
 
DROP TABLE WaterfallAdProvider;



