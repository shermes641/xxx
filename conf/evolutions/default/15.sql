# --- !Ups

CREATE OR REPLACE FUNCTION modify_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now() AT TIME ZONE 'UTC';;
    RETURN NEW;;
END;;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION modify_created_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.created_at = now() AT TIME ZONE 'UTC';;
    NEW.updated_at = now() AT TIME ZONE 'UTC';;
    RETURN NEW;;
END;;
$$ language 'plpgsql';

ALTER TABLE ad_providers ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE ad_providers ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER ad_providers_created_at_trigger BEFORE INSERT ON ad_providers FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER ad_providers_updated_at_trigger BEFORE UPDATE ON ad_providers FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE app_configs ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE app_configs ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER app_configs_created_at_trigger BEFORE INSERT ON app_configs FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER app_configs_updated_at_trigger BEFORE UPDATE ON app_configs FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE apps ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE apps ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER apps_created_at_trigger BEFORE INSERT ON apps FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER apps_updated_at_trigger BEFORE UPDATE ON apps FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE completions ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE completions ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER completions_created_at_trigger BEFORE INSERT ON completions FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER completions_updated_at_trigger BEFORE UPDATE ON completions FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE distributor_users ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE distributor_users ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER distributor_users_created_at_trigger BEFORE INSERT ON distributor_users FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER distributor_users_updated_at_trigger BEFORE UPDATE ON distributor_users FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE distributors ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE distributors ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER distributors_created_at_trigger BEFORE INSERT ON distributors FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER distributors_updated_at_trigger BEFORE UPDATE ON distributors FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE virtual_currencies ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE virtual_currencies ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER virtual_currencies_created_at_trigger BEFORE INSERT ON virtual_currencies FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER virtual_currencies_updated_at_trigger BEFORE UPDATE ON virtual_currencies FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE waterfall_ad_providers ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE waterfall_ad_providers ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER waterfall_ad_providers_created_at_trigger BEFORE INSERT ON waterfall_ad_providers FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER waterfall_ad_providers_updated_at_trigger BEFORE UPDATE ON waterfall_ad_providers FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

ALTER TABLE waterfalls ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE waterfalls ADD COLUMN created_at TIMESTAMP;

CREATE TRIGGER waterfalls_created_at_trigger BEFORE INSERT ON waterfalls FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER waterfalls_updated_at_trigger BEFORE UPDATE ON waterfalls FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

# --- !Downs

ALTER TABLE ad_providers DROP COLUMN created_at;
ALTER TABLE ad_providers DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS ad_providers_created_at_trigger on ad_providers;
DROP TRIGGER IF EXISTS ad_providers_updated_at_trigger on ad_providers;

ALTER TABLE app_configs DROP COLUMN created_at;
ALTER TABLE app_configs DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS app_configs_created_at_trigger on app_configs;
DROP TRIGGER IF EXISTS app_configs_updated_at_trigger on app_configs;

ALTER TABLE apps DROP COLUMN created_at;
ALTER TABLE apps DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS apps_created_at_trigger on apps;
DROP TRIGGER IF EXISTS apps_updated_at_trigger on apps;

ALTER TABLE completions DROP COLUMN created_at;
ALTER TABLE completions DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS completions_created_at_trigger on completions;
DROP TRIGGER IF EXISTS completions_updated_at_trigger on completions;

ALTER TABLE distributor_users DROP COLUMN created_at;
ALTER TABLE distributor_users DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS distributor_users_created_at_trigger on distributor_users;
DROP TRIGGER IF EXISTS distributor_users_updated_at_trigger on distributor_users;

ALTER TABLE distributors DROP COLUMN created_at;
ALTER TABLE distributors DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS distributors_created_at_trigger on distributors;
DROP TRIGGER IF EXISTS distributors_updated_at_trigger on distributors;

ALTER TABLE virtual_currencies DROP COLUMN created_at;
ALTER TABLE virtual_currencies DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS virtual_currencies_created_at_trigger on virtual_currencies;
DROP TRIGGER IF EXISTS virtual_currencies_updated_at_trigger on virtual_currencies;

ALTER TABLE waterfall_ad_providers DROP COLUMN created_at;
ALTER TABLE waterfall_ad_providers DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS waterfall_ad_providers_created_at_trigger on waterfall_ad_providers;
DROP TRIGGER IF EXISTS waterfall_ad_providers_updated_at_trigger on waterfall_ad_providers;

ALTER TABLE waterfalls DROP COLUMN created_at;
ALTER TABLE waterfalls DROP COLUMN updated_at;

DROP TRIGGER IF EXISTS waterfalls_created_at_trigger on waterfalls;
DROP TRIGGER IF EXISTS waterfalls_updated_at_trigger on waterfalls;
