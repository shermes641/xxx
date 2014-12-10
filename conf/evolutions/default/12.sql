# --- !Ups

ALTER TABLE distributors ADD COLUMN hypr_marketplace_id int;

# --- !Downs

ALTER TABLE distributors DROP COLUMN hypr_marketplace_id;
