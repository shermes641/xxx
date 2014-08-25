
# --- !Ups

CREATE TABLE waterfall_ad_providers (
  id bigserial,
  waterfall_id bigint,
  ad_provider_id bigint,
  waterfall_order int,
  cpm float,
  active BOOL NOT NULL DEFAULT TRUE,
  fill_rate float,
  PRIMARY KEY (id),
  FOREIGN KEY (waterfall_id) REFERENCES waterfalls(id),
  FOREIGN KEY (ad_provider_id) REFERENCES ad_providers(id)
);

CREATE INDEX waterfall_id_index on waterfall_ad_providers(waterfall_id);
CREATE INDEX ad_provider_id_index on waterfall_ad_providers(ad_provider_id);
CREATE UNIQUE INDEX ad_provider_id_waterfall_id_index on waterfall_ad_providers(ad_provider_id, waterfall_id);
 
# --- !Downs
 
DROP TABLE waterfall_ad_providers;



