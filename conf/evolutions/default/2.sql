# --- !Ups

CREATE TABLE users_watching_lots (
  id SERIAL PRIMARY KEY,
  user_id character varying(255) NOT NULL,
  lot_parameters_set_id integer NOT NULL
);

CREATE INDEX users_watching_lots_user_id on users_watching_lots (user_id);
CREATE INDEX users_watching_lots_lot_parameters_set_id on users_watching_lots (lot_parameters_set_id);
CREATE UNIQUE INDEX users_watching_lots_user_id_lot_parameters_set_id on users_watching_lots (user_id, lot_parameters_set_id);

CREATE TABLE users_banned_in_sales (
  id SERIAL PRIMARY KEY,
  user_id character varying(255) NOT NULL,
  sale_state_set_id integer NOT NULL
);

CREATE INDEX users_banned_in_sales_user_id on users_banned_in_sales (user_id);
CREATE INDEX users_banned_in_sales_sale_state_set_id on users_banned_in_sales (sale_state_set_id);
CREATE UNIQUE INDEX users_banned_in_sales_user_id_sale_state_set_id on users_banned_in_sales (user_id, sale_state_set_id);

# --- !Downs

DROP TABLE IF EXISTS users_watching_lots CASCADE;
DROP TABLE IF EXISTS users_banned_in_sales CASCADE;

