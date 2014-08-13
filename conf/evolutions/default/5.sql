
# --- !Ups

ALTER TABLE distributor_users ADD UNIQUE (email)

# --- !Downs

ALTER TABLE distributor_users DROP UNIQUE (email)



