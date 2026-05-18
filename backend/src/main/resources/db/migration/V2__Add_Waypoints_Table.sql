CREATE TABLE IF NOT EXISTS waypoints (
    id SERIAL PRIMARY KEY,
    tour_id INTEGER NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    order_index INTEGER NOT NULL
    );

CREATE INDEX idx_waypoints_tour_id ON waypoints(tour_id);