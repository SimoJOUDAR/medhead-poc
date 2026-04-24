# MedHead PoC — Backend

Spring Boot (Java 17, Maven) module of the MedHead PoC. Architecture, quick start, tests, CI, and the end-to-end walkthrough are documented in the [root `readme.md`](../readme.md); this file covers the backend's operational details: running PostgreSQL, building the container image, and preparing the local OSRM routing service.

## Running PostgreSQL via Docker Compose

The backend targets PostgreSQL 16 for local runs; integration tests spin up their own Testcontainers instance and are independent of the compose stack. The `docker-compose.yml` at the repo root declares a single `postgres` service (image `postgres:16-alpine`, published on `localhost:5432`, database `medhead`, user/password `medhead`/`medhead`).

```bash
# From the repo root. Start Postgres in the background.
docker compose up -d postgres

# Verify health.
docker compose ps postgres

# Tail logs if something's off.
docker compose logs -f postgres

# Stop without losing data.
docker compose stop postgres

# Stop and wipe the volume (rare -- resets the seed on next boot).
docker compose down -v
```

On first boot the backend runs `src/main/resources/schema.sql` and `data.sql` automatically through Spring's datasource initializer. The schema creates four tables (`specialty_groups`, `specialties`, `hospitals`, `hospital_specialties`) plus a partial index on available beds and a lat/long index. The seed installs the 12 NHS specialty groups, 80 specialties, 12 fictional UK hospitals, and the §2.6 fixture (Fred Brooks cardiology=2/immunology=3, Julia Crusher cardiology=0, Beverly Bashir immunology=5/Diagnostic neuropathology=4/Clinical radiology=2).

Override connection details via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Tests never hit the compose container -- they use a disposable Testcontainers instance bound through `@ServiceConnection`.

### Restoring the seed

To restore the seed without wiping the volume (e.g. after a scenario has zeroed bed counts):

```bash
# From the repo root.
docker cp backend/src/main/resources/data.sql medhead-postgres:/tmp/data.sql
docker exec -e PGPASSWORD=medhead medhead-postgres \
  psql -U medhead -d medhead -f /tmp/data.sql
```

The seed file uses `ON CONFLICT DO UPDATE` on `(hospital_id, specialty_id)` so re-application is idempotent and resets both `available_beds` and the optimistic-lock `version` column.

## Building the backend image

`Dockerfile` is a multi-stage build: a JDK layer compiles the JAR via the Maven wrapper, and a slim JRE layer runs it as a non-root user with a `/actuator/health` healthcheck. Build the image once, then run it against the Postgres started by `docker compose up -d postgres`:

```bash
# From the repo root.
docker build -t medhead-backend:dev backend

docker run --rm --name medhead-backend -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/medhead \
  -e SPRING_DATASOURCE_USERNAME=medhead \
  -e SPRING_DATASOURCE_PASSWORD=medhead \
  medhead-backend:dev
```

`host.docker.internal` is the Docker-host alias (built-in on Docker Desktop, declared by `--add-host` on Linux); it lets the containerised backend reach the Postgres published on the host's `5432`. Verify the boot with `curl -s localhost:8080/actuator/health` -- the response is `{"status":"UP"}` once Spring Boot has wired up the data source.

The `docker` stage of `.github/workflows/backend-ci.yml` performs this same build and boots the image as a smoke test on every push.

## Running OSRM locally

The backend computes road distances and travel times through a local [Open Source Routing Machine](https://project-osrm.org/) container. Integration and acceptance tests replace the adapter with a deterministic stub, so OSRM is **not required for `./mvnw -B verify`**; it is only needed when exercising `POST /api/v1/emergency/recommend` against a running backend.

Point the backend at a non-default host or port via `APP_OSRM_HOST`, `APP_OSRM_PORT`, and `APP_OSRM_READ_TIMEOUT_MS`. The defaults target the service declared in `docker-compose.yml` (`localhost:5000`).

OSRM needs a pre-processed `.osrm` graph on disk before `docker compose up` can start it. The full Great Britain extract (~700 MB PBF, several GB of processed artefacts) works, but a smaller region like Greater London is more than sufficient for the seeded fixtures -- all 12 hospitals are inside England. Map-data lives under `osrm-data/` at the repo root and is gitignored.

```bash
# All commands run from the repo root.

# 1. Download a region extract from Geofabrik (Greater London ≈ 80 MB).
mkdir -p osrm-data
curl -L -o osrm-data/greater-london-latest.osm.pbf \
  https://download.geofabrik.de/europe/united-kingdom/england/greater-london-latest.osm.pbf

# 2. Extract / partition / customize (one-off).
docker run --rm -t -v "$PWD/osrm-data:/data" osrm/osrm-backend \
  osrm-extract -p /opt/car.lua /data/greater-london-latest.osm.pbf
docker run --rm -t -v "$PWD/osrm-data:/data" osrm/osrm-backend \
  osrm-partition /data/greater-london-latest.osrm
docker run --rm -t -v "$PWD/osrm-data:/data" osrm/osrm-backend \
  osrm-customize /data/greater-london-latest.osrm

# 3. Start the service, pointing at the produced graph.
OSRM_GRAPH=greater-london-latest.osrm docker compose up -d osrm
```

Confirm the server is up with `curl "http://localhost:5000/route/v1/driving/-0.131,51.523;-0.130,51.523?overview=false"`; expect a JSON payload with a `routes[0].distance` field in metres.

## See also

- [Root `readme.md`](../readme.md) — quick start, tests, CI/CD pipeline, branch workflow, end-to-end walkthrough.
- [`jmeter/README.md`](jmeter/README.md) — stress harness (JMeter plan, runner, Phase A/B reports).
- [`../postman/README.md`](../postman/README.md) — Newman-runnable API collection.
