# --- !Ups

/*
  Update any NULL reward_min records before setting NOT NULL constraint.
 */
UPDATE virtual_currencies SET reward_min = 1 WHERE reward_min IS NULL;

ALTER TABLE virtual_currencies ALTER COLUMN reward_min SET NOT NULL;
ALTER TABLE virtual_currencies ALTER COLUMN reward_min SET DEFAULT 1;

# --- !Downs

ALTER TABLE virtual_currencies ALTER COLUMN reward_min DROP NOT NULL;
ALTER TABLE virtual_currencies ALTER COLUMN reward_min DROP DEFAULT;
