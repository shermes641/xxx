
# --- !Ups

CREATE TABLE waterfall_ad_providers (
  id bigserial,
  waterfall_id bigint,
  ad_provider_id bigint,
  waterfall_order int,
  cpm float,
  active bit(1),
  fill_rate float,
  PRIMARY KEY (id),
  FOREIGN KEY (waterfall_id) REFERENCES Waterfall(id),
  FOREIGN KEY (ad_provider_id) REFERENCES AdProvider(id)
);

CREATE INDEX waterfall_id_index on waterfall_ad_providers(waterfall_id);
CREATE INDEX ad_provider_id_index on waterfall_ad_providers(ad_provider_id);
 
# --- !Downs
 
DROP TABLE waterfall_ad_providers;



