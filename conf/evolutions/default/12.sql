# --- !Ups

ALTER TABLE distributor_users ADD COLUMN hypr_marketplace_id int;

# --- !Downs

ALTER TABLE distributor_users DROP COLUMN hypr_marketplace_id;
