-- Inserts the "Dummy Tour" for the mobile team to test immediately
INSERT INTO tours (
    name,
    start_lat, start_lon,
    end_lat, end_lon,
    share_code,
    distance,
    duration,
    geometry
) VALUES (
     'Tour Test Mobile',
     47.218371, -1.553621, -- Nantes Centre
     47.218371, -1.553621, -- Nantes Centre (Loop)
     'C15ROC',             -- THE CODE
     12500.0,              -- ~12.5 km
     1800.0,               -- 30 min
     -- polyline (Zone de Nantes)
     'u~~BR_}c@?A@?@?@?A@?@?A@?@?A@?@?@?@?A@?@?@?A?@?A@?@?@?@?A@?@?@?A@?@?A@?@?@?'
);