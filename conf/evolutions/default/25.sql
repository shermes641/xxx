# --- !Ups

CREATE TABLE platforms (
  id serial PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TRIGGER platforms_created_at_trigger BEFORE INSERT ON platforms FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER platforms_updated_at_trigger BEFORE UPDATE ON platforms FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE apps ADD COLUMN platform_id int;
ALTER TABLE apps ADD CONSTRAINT apps_platform_id_fkey FOREIGN KEY (platform_id) REFERENCES platforms(id);
CREATE INDEX apps_platform_id_index on apps(platform_id);

ALTER TABLE ad_providers ADD COLUMN platform_id int;
ALTER TABLE ad_providers ADD CONSTRAINT ad_providers_platform_id_fkey FOREIGN KEY (platform_id) REFERENCES platforms(id);
CREATE INDEX ad_providers_platform_id_index on ad_providers(platform_id);
CREATE UNIQUE INDEX ad_provider_name_platform_id_index on ad_providers(LOWER(name), platform_id);

INSERT INTO platforms (name) VALUES ('iOS');
INSERT INTO platforms (name) VALUES ('Android');

UPDATE apps SET platform_id = 1;
UPDATE ad_providers SET platform_id = 1;

ALTER TABLE apps ALTER COLUMN platform_id SET NOT NULL;
ALTER TABLE ad_providers ALTER COLUMN platform_id SET NOT NULL;

DROP INDEX IF EXISTS active_distributor_app_name;
CREATE UNIQUE INDEX active_distributor_app_name_platform_id on apps(LOWER(name), distributor_id, active, platform_id);

# --- !Downs

DELETE FROM waterfall_ad_providers WHERE ad_provider_id IN (
  SELECT id FROM ad_providers
  WHERE platform_id = 2
);

DELETE FROM waterfalls WHERE waterfalls.app_id IN (
  SELECT apps.id FROM apps
  WHERE apps.platform_id = 2
);

DELETE FROM app_configs WHERE app_configs.app_id IN (
  SELECT apps.id from apps
  WHERE apps.platform_id = 2
);

DELETE FROM virtual_currencies WHERE virtual_currencies.app_id IN (
  SELECT apps.id from apps
  WHERE apps.platform_id = 2
);

DELETE FROM apps WHERE platform_id = 2;

DELETE FROM ad_providers WHERE platform_id = 2;

ALTER TABLE apps DROP COLUMN IF EXISTS platform_id;

ALTER TABLE ad_providers DROP COLUMN IF EXISTS platform_id;

DROP TABLE IF EXISTS platforms;

DROP INDEX IF EXISTS ad_provider_name_platform_id_index;
DROP INDEX IF EXISTS ad_providers_platform_id_index;
DROP INDEX IF EXISTS apps_platform_id_index;
DROP INDEX IF EXISTS active_distributor_app_name_platform_id;
CREATE UNIQUE INDEX active_distributor_app_name on apps(LOWER(name), distributor_id, active);
