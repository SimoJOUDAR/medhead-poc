CREATE TABLE IF NOT EXISTS specialty_groups (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS specialties (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(150) NOT NULL,
    group_id BIGINT NOT NULL REFERENCES specialty_groups(id),
    UNIQUE (name, group_id)
);

CREATE TABLE IF NOT EXISTS hospitals (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(200) NOT NULL,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address   VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS hospital_specialties (
    id             BIGSERIAL PRIMARY KEY,
    hospital_id    BIGINT  NOT NULL REFERENCES hospitals(id),
    specialty_id   BIGINT  NOT NULL REFERENCES specialties(id),
    available_beds INTEGER NOT NULL DEFAULT 0 CHECK (available_beds >= 0),
    version        BIGINT  NOT NULL DEFAULT 0,
    UNIQUE (hospital_id, specialty_id)
);

CREATE INDEX IF NOT EXISTS idx_hospital_specialty_available
    ON hospital_specialties (specialty_id)
    WHERE available_beds > 0;

CREATE INDEX IF NOT EXISTS idx_hospitals_coords
    ON hospitals (latitude, longitude);
