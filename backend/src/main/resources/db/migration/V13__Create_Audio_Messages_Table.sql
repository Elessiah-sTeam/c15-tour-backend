CREATE TABLE audio_messages (
    id         SERIAL PRIMARY KEY,
    tour_id    INTEGER NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    file_name  TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
