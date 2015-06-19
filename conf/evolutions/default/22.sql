# --- !Ups

DROP INDEX IF EXISTS active_distributor_app_name;
CREATE UNIQUE INDEX active_distributor_app_name on apps(LOWER(name), distributor_id, active);

# --- !Downs

DROP INDEX IF EXISTS active_distributor_app_name;
CREATE UNIQUE INDEX active_distributor_app_name on apps(name, distributor_id, active);
