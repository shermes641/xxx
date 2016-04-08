# --- !Ups

CREATE SEQUENCE roles_id_seq;

CREATE TABLE roles (
  id int PRIMARY KEY DEFAULT nextval('roles_id_seq'),
  name varchar(255) NOT NULL UNIQUE
);

CREATE SEQUENCE permissions_id_seq;

CREATE TABLE permissions (
  id int PRIMARY KEY DEFAULT nextval('permissions_id_seq'),
  value varchar(255) NOT NULL
);

CREATE SEQUENCE distributor_users_roles_id_seq;

CREATE TABLE distributor_users_roles (
  id int PRIMARY KEY DEFAULT nextval('distributor_users_roles_id_seq'),
  distributor_user_id bigint NOT NULL,
  role_id int NOT NULL,
  FOREIGN KEY (role_id) REFERENCES roles(id),
  FOREIGN KEY (distributor_user_id) REFERENCES distributor_users(id)
);

CREATE INDEX distributor_users_roles_role_id_index on distributor_users_roles(role_id);
CREATE INDEX distributor_users_roles_distributor_user_id_index on distributor_users_roles(distributor_user_id);

CREATE SEQUENCE roles_permissions_id_seq;

CREATE TABLE roles_permissions (
  id int PRIMARY KEY DEFAULT nextval('roles_permissions_id_seq'),
  permission_id int NOT NULL,
  role_id int NOT NULL,
  FOREIGN KEY (role_id) REFERENCES roles(id),
  FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE INDEX roles_permissions_role_id_index on roles_permissions(role_id);
CREATE INDEX roles_permissions_permission_id_index on roles_permissions(permission_id);

INSERT INTO roles (name) VALUES('ADMIN');
INSERT INTO roles (name) VALUES('OPS');
INSERT INTO roles (name) VALUES('QA');
INSERT INTO roles (name) VALUES('AD_PROVIDER');

# --- !Downs

DROP TABLE roles_permissions;
DROP TABLE distributor_users_roles;
DROP TABLE roles;
DROP TABLE permissions;

DROP SEQUENCE roles_permissions_id_seq;
DROP SEQUENCE distributor_users_roles_id_seq;
DROP SEQUENCE permissions_id_seq;
DROP SEQUENCE roles_id_seq;
