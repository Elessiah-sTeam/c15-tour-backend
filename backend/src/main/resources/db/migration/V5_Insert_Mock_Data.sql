INSERT INTO tours (
    name,
    start_lat, start_lon,
    end_lat, end_lon,
    share_code,
    distance,
    duration,
    geometry
) VALUES (
             'Trip 4-Step Dummy',
             47.218371, -1.553621,
             46.974400, -1.312600,
             'C15ROD',
             35000.0,
             2400.0,
             -- The 4 coordinates as a JSON text string
             '[
                [47.218371, -1.553621],
                [47.180000, -1.540000],
                [47.070000, -1.400000],
                [46.974400, -1.312600]
             ]'
         );