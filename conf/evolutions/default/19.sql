# --- !Ups

ALTER TABLE completions ADD COLUMN reward_quantity bigint;
ALTER TABLE completions ADD COLUMN generation_number bigint;
CREATE INDEX app_configs_generation_number_index on app_configs(generation_number);

# --- !Downs

ALTER TABLE completions DROP COLUMN reward_quantity;
ALTER TABLE completions DROP COLUMN generation_number;
DROP INDEX IF EXISTS app_configs_generation_number_index;
