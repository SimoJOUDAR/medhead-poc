# MedHead PoC -- Real-Time Emergency Hospital Bed Allocation

> Proof of Concept for the MedHead Consortium's real-time emergency response system.
> Recommends the nearest hospital with an available bed in the required medical specialty.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Running the Tests](#running-the-tests)
- [CI/CD Pipeline](#cicd-pipeline)
- [Branch workflow](#branch-workflow)
- [Accessibility](#accessibility)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

## Overview

<!-- To be completed in sessions S2-S5 -->

## Architecture

<!-- To be completed in sessions S2-S5 -->

## Prerequisites

<!-- To be completed in session S2 -->

## Getting Started

<!-- To be completed in session S2 -->

## Running the Application

<!-- To be completed in session S2 -->

### Running PostgreSQL via Docker Compose

The backend targets PostgreSQL 16 for local runs; integration tests spin up their own Testcontainers instance and are independent of the compose stack. The `docker-compose.yml` at the repo root declares a single `postgres` service (image `postgres:16-alpine`, published on `localhost:5432`, database `medhead`, user/password `medhead`/`medhead`).

```bash
# Start Postgres in the background.
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

On first boot the backend runs `backend/src/main/resources/schema.sql` and `data.sql` automatically through Spring's datasource initializer. The schema creates four tables (`specialty_groups`, `specialties`, `hospitals`, `hospital_specialties`) plus a partial index on available beds and a lat/long index. The seed installs the 12 NHS specialty groups, 80 specialties, 12 fictional UK hospitals, and the §2.6 fixture (Fred Brooks cardiology=2/immunology=3, Julia Crusher cardiology=0, Beverly Bashir immunology=5/Diagnostic neuropathology=4/Clinical radiology=2).

Override connection details via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Tests never hit the compose container -- they use a disposable Testcontainers instance bound through `@ServiceConnection`.

To restore the seed without wiping the volume (e.g. after a scenario has zeroed bed counts):

```bash
docker cp backend/src/main/resources/data.sql medhead-postgres:/tmp/data.sql
docker exec -e PGPASSWORD=medhead medhead-postgres \
  psql -U medhead -d medhead -f /tmp/data.sql
```

The seed file uses `ON CONFLICT DO UPDATE` on `(hospital_id, specialty_id)` so re-application is idempotent and resets both `available_beds` and the optimistic-lock `version` column.

### Running OSRM locally

The backend computes road distances and travel times through a local
[Open Source Routing Machine](https://project-osrm.org/) container. Integration
and acceptance tests replace the adapter with a deterministic stub, so OSRM is
**not required for `./mvnw -B verify`**; it is only needed when exercising
`POST /api/v1/emergency/recommend` against a running backend.

Point the backend at a non-default host or port via `APP_OSRM_HOST`,
`APP_OSRM_PORT`, and `APP_OSRM_READ_TIMEOUT_MS`. The defaults target the
service declared in `docker-compose.yml` (`localhost:5000`).

OSRM needs a pre-processed `.osrm` graph on disk before `docker compose up`
can start it. The full Great Britain extract (~700 MB PBF, several GB of
processed artefacts) works, but a smaller region like Greater London is more
than sufficient for the seeded fixtures -- all 12 hospitals are inside
England. Map-data lives under `osrm-data/` and is gitignored.

```bash
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

Confirm the server is up with
`curl "http://localhost:5000/route/v1/driving/-0.131,51.523;-0.130,51.523?overview=false"`;
expect a JSON payload with a `routes[0].distance` field in metres.

### Local dev credentials

The backend ships a single pre-seeded user for local development and demo purposes:

| Username | Password |
|----------|----------|
| `demo`   | `demo`   |

Exchange them for a JWT via `POST /api/v1/auth/login`:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo"}'
```

The response payload includes a `token` field; send it as `Authorization: Bearer <token>` on every subsequent `/api/v1/**` call. Tokens are HS256-signed and expire after 60 minutes by default.

Production deployments must override the signing secret and user store via environment variables (`APP_SECURITY_JWT_SECRET`, `APP_SECURITY_USERS_0_PASSWORD`, `APP_SECURITY_JWT_TTL_MINUTES`). The default secret shipped in `application.yaml` is flagged as dev-only and must not leave a developer machine.

### End-to-end walkthrough: the main recommendation scenario

The canonical acceptance scenario takes a cardiology emergency near Fred Brooks Hospital, recommends Fred Brooks, reserves a bed, and publishes a bed-reservation event. All the moving parts are exercisable with plain `curl` + `docker exec` once Postgres and the backend are up.

```bash
# 1. Start Postgres and boot the backend (separate terminals).
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run

# 2. Acquire a JWT.
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo"}' | jq -r .token)

# 3. Look up the Cardiology specialty id from the catalogue.
CARDIOLOGY_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/specialties \
  | jq '.[] | select(.name=="Cardiology") | .id')

# 4. Inspect Fred Brooks' cardiology bed count before the call.
docker exec -e PGPASSWORD=medhead medhead-postgres \
  psql -U medhead -d medhead -tA -c "
    SELECT hs.available_beds
      FROM hospital_specialties hs
      JOIN hospitals  h ON hs.hospital_id  = h.id
      JOIN specialties s ON hs.specialty_id = s.id
     WHERE h.name = 'Fred Brooks Hospital'
       AND s.name = 'Cardiology';"
# Expected: 2

# 5. Request a recommendation near Fred Brooks (lat 51.523, lon -0.131).
curl -s -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -X POST http://localhost:8080/api/v1/emergency/recommend \
  -d "{\"specialtyId\":$CARDIOLOGY_ID,\"latitude\":51.523,\"longitude\":-0.131}" \
  | jq
# Expected: hospital.name == "Fred Brooks Hospital",
#           specialty.name == "Cardiology",
#           fallback == false, bedReserved == true,
#           hospital.availableBeds == 1
```

Two side-effects should be observable:

- The backend log shows `[BED_RESERVATION] hospitalId=1 hospital="Fred Brooks Hospital" specialtyId=<...> specialty="Cardiology" remainingBeds=1` -- the Spring `ApplicationEvent` emitted by the reservation flow.
- Re-running the psql query from step 4 returns `1` -- the bed has been decremented atomically under optimistic locking.

Restore the seed with the `docker cp` + `psql -f` recipe from the [Running PostgreSQL](#running-postgresql-via-docker-compose) section when you're done experimenting.

## Running the Tests

<!-- To be completed in session S4 -->

### Non-functional tests

Stress coverage lives outside the Maven build so a slow or flaky load run never blocks the per-PR `./mvnw -B verify` loop. The JMeter test plan, its runner script, and the prerequisites checklist (JMeter 5.6+, Postgres JDBC driver on the JMeter classpath, full stack booted) are all documented in [`backend/jmeter/README.md`](backend/jmeter/README.md). One-line reminder:

```bash
cd backend/jmeter && ./run-baseline.sh
```

The run produces an HTML dashboard under `backend/jmeter/report/` whose per-endpoint p50 / p95 / p99 + throughput numbers are the inputs to the Phase A verdict. The filled report for the committed reference run is in [`backend/jmeter/phase_a_baseline.md`](backend/jmeter/phase_a_baseline.md); the raw samples (`results.jtl`) and dashboard (`report/index.html`) are committed alongside it so the numbers are independently verifiable.

### Running coverage locally

Coverage is observed, not gated -- the build does not fail on a low number. JaCoCo on the backend merges Surefire (unit) + Failsafe (integration + Cucumber) execution data into a single aggregate report; Vitest on the frontend uses the v8 provider.

```bash
# Backend -- aggregate report at backend/target/site/jacoco-aggregate/index.html
cd backend && ./mvnw -B clean verify

# Frontend -- report at frontend/coverage/index.html
cd frontend && npm run test:coverage
```

The backend aggregate run is the one wired into `./mvnw -B verify`; no extra flag is required. Artefact upload + threshold gating are deferred to the CI hardening pass.

### API tooling (Postman)

The full S5 API surface is exercised by a Newman-runnable Postman collection under [`postman/`](postman/README.md): JWT login (with a token-stash test script populating the `{{token}}` environment variable), the specialty and hospital catalogues, single hospital lookup, and the emergency recommendation endpoint on both the §2.6 main scenario and the fallback path. Every request carries `pm.test` assertions on its status code and at least one shape field so the collection doubles as a self-checking smoke test.

```bash
# Boot the stack first.
docker compose up -d postgres osrm
cd backend && ./mvnw spring-boot:run

# Then, from the repo root, run the collection headlessly.
newman run postman/medhead-poc.postman_collection.json \
  -e postman/medhead-poc.postman_environment.json
```

Import / interactive-use instructions and a note on side-effects (each successful main-scenario call reserves a Fred Brooks cardiology bed) are in [`postman/README.md`](postman/README.md).

## CI/CD Pipeline

<!-- To be completed in session S5 -->

## Branch workflow

The project follows **GitHub Flow**: `master` is the only long-lived branch and is always deployable. Every change lands through a short-lived topic branch and a pull request.

### Branching

- Cut topic branches from `master`; delete them after merge (one branch per PR).
- Branch names use `type/short-description`, kebab-case, where `type` is one of `feat`, `fix`, `ci`, `docs`, `test`, `chore`, `refactor` (for example `ci/backend-workflow`, `feat/bed-availability`).

### Merging to `master`

Direct pushes to `master` are disabled. Every change reaches `master` through a pull request that satisfies all of the following, enforced by branch protection:

- The `backend-ci` and `frontend-ci` status checks are green.
- The branch is up to date with `master` before merging.
- Force-pushes and deletion of `master` are rejected.

### Commits

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `ci:`, `docs:`, `test:`, `chore:`, `refactor:`) so history stays scannable and can drive release notes later.

## Accessibility

The front-end targets **WCAG 2.1 AA**. Compliance is enforced automatically and verified manually.

### Automated check

`frontend/src/__tests__/accessibility.test.tsx` runs [`jest-axe`](https://github.com/nickcolley/jest-axe) against the two main UI states -- the unauthenticated login form and the authenticated recommendation form -- and asserts zero violations against the `wcag2a`, `wcag2aa`, `wcag21a`, and `wcag21aa` rule tags. The test runs as part of `npm test` and `frontend-ci`.

### Manual sweep

Run through the following before any demo; the steps passed on the last audit.

- **Keyboard-only flow.** From a cold page load, `Tab` reaches every interactive element in reading order -- unauthenticated: username → password → submit; authenticated: specialty → latitude → longitude → submit → log out. `Shift+Tab` walks the reverse order. `Enter` submits the active form; `Space` toggles buttons; arrow keys navigate the specialty `<select>`. No focus trap, no keyboard dead-ends.
- **Visible focus indicator.** The default user-agent focus ring is preserved (no `outline: none` overrides); every interactive element shows a visible focus state when reached via keyboard.
- **Colour contrast.** Body text (`--text` on `--bg`) and headings (`--text-h` on `--bg`) exceed the 4.5:1 ratio required for normal text in both light and dark schemes.
- **Screen-reader labelling.** VoiceOver (macOS) and NVDA (Windows) announce every control with its role and label: inputs via their bound `<label htmlFor>`, the select via its label + group structure, the submit buttons via their accessible name, the error region via `role="alert"` + `aria-live="assertive"`, and the result card via its `<h2>` tied through `aria-labelledby`.

## API Documentation

<!-- To be completed in session S2 -->

## Project Structure

<!-- To be completed in session S2 -->
