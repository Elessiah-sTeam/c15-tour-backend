ALTER TABLE tours ADD COLUMN share_code VARCHAR(6);
ALTER TABLE tours ADD CONSTRAINT uk_tours_share_code UNIQUE (share_code);

ALTER TABLE tours ADD COLUMN geometry TEXT;


ALTER TABLE tours ADD COLUMN distance DOUBLE PRECISION; -- en mètres
ALTER TABLE tours ADD COLUMN duration DOUBLE PRECISION; -- en secondes