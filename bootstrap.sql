CREATE TABLE measure_point_dim (
	measure_point_sk serial PRIMARY KEY,
	measure_point_id int not null,
	name text NOT NULL
);

DO $$
    BEGIN
        FOR loop_counter IN 1..100 LOOP
            INSERT INTO measure_point_dim(measure_point_id, name) VALUES (loop_counter, 'mp' || loop_counter);
        END LOOP;
    END;
$$;

CREATE TABLE asset_dim (
	asset_sk serial PRIMARY KEY,
	asset_id int not null,
	name text NOT NULL
);

DO $$
    BEGIN
        FOR loop_counter IN 1..100 LOOP
            INSERT INTO asset_dim(asset_id, name) VALUES (loop_counter, 'asset' || loop_counter);
        END LOOP;
    END;
$$;

CREATE TABLE readings (
    asset_sk int REFERENCES asset_dim(asset_sk),
    measure_point_sk int REFERENCES measure_point_dim(measure_point_sk),
	timestamp bigint NOT NULL,
	value numeric NOT NULL,
	PRIMARY KEY (asset_sk, measure_point_sk, timestamp)
);
