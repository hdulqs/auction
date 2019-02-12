-- !Ups

CREATE TABLE bids_placed (
    id SERIAL PRIMARY KEY,
    event_id character varying(255) NOT NULL,
    lot_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    bidder_id character varying(255) NOT NULL,
    bid_type character varying(255) NOT NULL,
    amount_cents bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE derived_lot_states (
    id SERIAL PRIMARY KEY,
    lot_id character varying(255) NOT NULL,
    event_id character varying(255) NOT NULL,
    app_version character varying(255) NOT NULL,
    state_json jsonb NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE increment_policy_revision_set (
    id character varying(255) NOT NULL,
    group_tag text NOT NULL,
    subgroup_tag text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    initial_increment_cents bigint NOT NULL,
    changes_json text NOT NULL
);

CREATE TABLE lot_parameters_set (
    id SERIAL PRIMARY KEY,
    external_id character varying(255) NOT NULL,
    sale_state_set_id integer NOT NULL,
    initial_starting_price_cents bigint NOT NULL,
    initial_reserve_cents bigint NOT NULL,
    initial_increment_policy_id character varying(255) DEFAULT 'default'::character varying NOT NULL,
    estimate_value_cents bigint,
    low_estimate_cents bigint,
    high_estimate_cents bigint,
    initial_max_bid_rule character varying(255) DEFAULT 'EarlyHighBidderPriority'::character varying NOT NULL,
    initial_next_increment_rule character varying(255) DEFAULT 'AddToPastValue'::character varying NOT NULL,
    CONSTRAINT lot_parameters_set_initial_max_bid_rule_check CHECK ((((initial_max_bid_rule)::text = 'EarlyHighBidderPriority'::text) OR ((initial_max_bid_rule)::text = 'NoEarlyHighBidderPriority'::text))),
    CONSTRAINT lot_parameters_set_initial_next_increment_rule_check CHECK ((((initial_next_increment_rule)::text = 'AddToPastValue'::text) OR ((initial_next_increment_rule)::text = 'SnapToPresetIncrements'::text))),
    CONSTRAINT valid_estimate_range CHECK ((((estimate_value_cents IS NULL) AND (low_estimate_cents IS NULL) AND (high_estimate_cents IS NULL)) OR ((estimate_value_cents IS NOT NULL) AND (low_estimate_cents IS NULL) AND (high_estimate_cents IS NULL)) OR ((estimate_value_cents IS NULL) AND (low_estimate_cents IS NOT NULL) AND (high_estimate_cents IS NOT NULL))))
);

CREATE UNIQUE INDEX lot_parameters_set_external_id ON lot_parameters_set USING btree (external_id);

CREATE TABLE lots (
    lot_id character varying(255) PRIMARY KEY REFERENCES lot_parameters_set(external_id),
    events_json jsonb NOT NULL
);

CREATE TABLE sale_state_set (
    id SERIAL PRIMARY KEY,
    external_id character varying(255) NOT NULL,
    current_lot_id integer
);

CREATE TABLE users_banned_in_sales (
    id SERIAL PRIMARY KEY,
    user_id character varying(255) NOT NULL,
    sale_state_set_id integer NOT NULL
);

CREATE TABLE users_watching_lots (
    id SERIAL PRIMARY KEY,
    user_id character varying(255) NOT NULL,
    lot_parameters_set_id integer NOT NULL
);

CREATE INDEX bids_placed_bidder_id ON bids_placed USING btree (bidder_id);
CREATE INDEX bids_placed_created_at ON bids_placed USING btree (created_at);
CREATE INDEX bids_placed_lot_id ON bids_placed USING btree (lot_id);
CREATE INDEX bids_placed_user_id ON bids_placed USING btree (user_id);

CREATE INDEX derived_lot_states_app_version ON derived_lot_states USING btree (app_version);
CREATE INDEX derived_lot_states_created_at ON derived_lot_states USING btree (created_at);
CREATE INDEX derived_lot_states_event_id ON derived_lot_states USING btree (event_id);
CREATE INDEX derived_lot_states_json ON derived_lot_states USING gin (state_json);
CREATE INDEX derived_lot_states_lot_id ON derived_lot_states USING btree (lot_id);

CREATE UNIQUE INDEX increment_policy_revision_set_id ON increment_policy_revision_set USING btree (id);

CREATE INDEX lot_parameters_set_sale_state_set_id ON lot_parameters_set USING btree (sale_state_set_id);

CREATE UNIQUE INDEX sale_state_set_external_id ON sale_state_set USING btree (external_id);

CREATE INDEX users_banned_in_sales_sale_state_set_id ON users_banned_in_sales USING btree (sale_state_set_id);
CREATE INDEX users_banned_in_sales_user_id ON users_banned_in_sales USING btree (user_id);
CREATE UNIQUE INDEX users_banned_in_sales_user_id_sale_state_set_id ON users_banned_in_sales USING btree (user_id, sale_state_set_id);

CREATE INDEX users_watching_lots_lot_parameters_set_id ON users_watching_lots USING btree (lot_parameters_set_id);
CREATE INDEX users_watching_lots_user_id ON users_watching_lots USING btree (user_id);
CREATE UNIQUE INDEX users_watching_lots_user_id_lot_parameters_set_id ON users_watching_lots USING btree (user_id, lot_parameters_set_id);

ALTER TABLE ONLY bids_placed
    ADD CONSTRAINT bids_placed_lot_id_fkey FOREIGN KEY (lot_id) REFERENCES lot_parameters_set(external_id);

ALTER TABLE ONLY derived_lot_states
    ADD CONSTRAINT derived_lot_states_lot_id_fkey FOREIGN KEY (lot_id) REFERENCES lot_parameters_set(external_id);

ALTER TABLE ONLY lot_parameters_set
    ADD CONSTRAINT lot_parameters_set_sale_state_set_id_fkey FOREIGN KEY (sale_state_set_id) REFERENCES sale_state_set(id) ON DELETE CASCADE;


ALTER TABLE ONLY sale_state_set
    ADD CONSTRAINT sale_state_set_current_lot_id_fkey FOREIGN KEY (current_lot_id) REFERENCES lot_parameters_set(id);

-- !Downs

DROP TABLE bids_placed CASCADE;
DROP TABLE derived_lot_states CASCADE;
DROP TABLE increment_policy_revision_set CASCADE;
DROP TABLE lot_parameters_set CASCADE;
DROP TABLE lots CASCADE;
DROP TABLE sale_state_set CASCADE;
DROP TABLE users_banned_in_sales CASCADE;
DROP TABLE users_watching_lots CASCADE;
