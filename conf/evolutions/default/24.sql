# --- !Ups

CREATE TABLE password_resets (
  id bigserial PRIMARY KEY,
  token VARCHAR(255) NOT NULL DEFAULT uuid_generate_v4(),
  distributor_user_id bigint,
  completed BOOL NOT NULL DEFAULT FALSE,
  FOREIGN KEY (distributor_user_id) REFERENCES distributor_users(id),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TRIGGER password_resets_created_at_trigger BEFORE INSERT ON password_resets FOR EACH ROW EXECUTE PROCEDURE modify_created_at_column();
CREATE TRIGGER password_resets_updated_at_trigger BEFORE UPDATE ON password_resets FOR EACH ROW EXECUTE PROCEDURE modify_updated_at_column();

CREATE INDEX password_resets_token_index on password_resets(token);
CREATE INDEX password_resets_distributor_user_id_index on password_resets(distributor_user_id);
CREATE UNIQUE INDEX distributor_user_id_token_index on password_resets(token, distributor_user_id);

# --- !Downs

DROP TABLE password_resets;
