# --- First database schema

# --- !Ups

CREATE TABLE sale_state_set (
  id SERIAL PRIMARY KEY,
  external_id character varying(255) NOT NULL
);

CREATE UNIQUE INDEX sale_external_id on sale (external_id);

CREATE TABLE increment_policy (
  id SERIAL PRIMARY KEY,
  external_id character varying(255) NOT NULL,
  initial_increment_cents bigint NOT NULL
);

CREATE UNIQUE INDEX increment_policy_external_id on increment_policy (external_id);

CREATE TABLE increment_change (
  id SERIAL PRIMARY KEY,
  increment_policy_id integer NOT NULL REFERENCES increment_policy (id) ON DELETE CASCADE,
  threshold_cents bigint NOT NULL,
  new_increment_cents bigint NOT NULL
);

CREATE INDEX increment_change_increment_policy_id on increment_change (increment_policy_id);

CREATE TABLE lot_parameter (
  id SERIAL PRIMARY KEY,
  external_id character varying(255) NOT NULL,
  sale_id integer NOT NULL REFERENCES sale (id) ON DELETE CASCADE,
  auction_type character varying(255) NOT NULL,
  initial_starting_price_cents bigint NOT NULL,
  initial_reserve_cents bigint NOT NULL,
  open_time timestamp without time zone NOT NULL,
  close_time timestamp without time zone NOT NULL,
  increment_policy_id integer NOT NULL REFERENCES increment_policy (id) ON DELETE CASCADE
);

CREATE INDEX lot_parameter_increment_policy_id on lot_parameter (increment_policy_id);
CREATE INDEX lot_parameter_sale_id on lot_parameter (sale_id);
CREATE UNIQUE INDEX lot_parameter_external_id on lot_parameter (external_id);

ALTER TABLE sale ADD COLUMN current_lot_id integer REFERENCES lot_parameter (id);


# --- !Downs

DROP TABLE IF EXISTS sale CASCADE;
DROP TABLE IF EXISTS increment_policy CASCADE;
DROP TABLE IF EXISTS increment_change CASCADE;
DROP TABLE IF EXISTS lot_parameter CASCADE;

