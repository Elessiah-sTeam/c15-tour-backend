DELETE FROM tours;
DELETE FROM waypoints;

ALTER TABLE tours
    DROP COLUMN start_lat,
    DROP COLUMN start_lon,
    DROP COLUMN end_lat,
    DROP COLUMN end_lon,
    DROP COLUMN geometry,
    RENAME COLUMN distance TO total_distance,
    RENAME COLUMN duration TO total_duration

CREATE TABLE IF NOT EXISTS segment (
     id SERIAL PRIMARY KEY,

     tour_id INTEGER NOT NULL,

     name TEXT NOT NULL,
     distance INTEGER NOT NULL,
     duration INTEGER NOT NULL, -- secondes

     geometry JSONB,

     order_index INTEGER NOT NULL,

     CONSTRAINT fk_segment_tour
         FOREIGN KEY (tour_id)
             REFERENCES tour(id)
             ON DELETE CASCADE
);

ALTER TABLE waypoints DROP COLUMN tour_id;
ALTER TABLE waypoints ADD COLUMN segment_id INTEGER REFERENCES segment(id) ON DELETE CASCADE;

