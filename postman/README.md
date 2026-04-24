# MedHead PoC -- Postman collection

A Newman-runnable Postman collection covering every endpoint exposed by the backend: JWT login (with a token-stash test script), the specialty catalogue, the hospital catalogue, single hospital lookup, and the emergency recommendation endpoint exercised on both the §2.6 main scenario and the fallback path.

Each request carries a `pm.test` assertion on its status code and at least one shape field, so the collection doubles as a self-checking smoke test rather than a click-through demo.

## Files

| File | Purpose |
|------|---------|
| `medhead-poc.postman_collection.json` | The collection itself (Postman v2.1.0 schema). |
| `medhead-poc.postman_environment.json` | The `local` environment with `baseUrl`, `username`, `password`, and an empty `token` populated by the login request's test script. |

## Prerequisites

The collection talks to a fully-booted backend at `http://localhost:8080`. Bring the stack up first:

```bash
docker compose up -d postgres osrm
cd backend && ./mvnw spring-boot:run
```

The seeded `demo` / `demo` credentials are documented in the project root's [`readme.md`](../readme.md); the §2.6 fixture (Fred Brooks Hospital, cardiology specialty `id=21`) is detailed in [`backend/README.md`](../backend/README.md#running-postgresql-via-docker-compose). The fallback request targets specialty `id=60` (Dental public health), which has no rows in `hospital_specialties` so the recommendation widens to the nearest hospital with any bed.

## Running interactively in Postman Desktop

1. Open Postman Desktop.
2. **File -> Import** (or drag-and-drop) both JSON files. Postman recognises them as a collection and an environment respectively.
3. Top-right environment selector -> pick **MedHead PoC -- local**.
4. Run the **Login (acquire JWT)** request. The `Tests` tab assertions go green; the environment quick-look (eye icon) shows `token` populated.
5. Run each remaining request top-to-bottom. Every one should be 200; the fallback request returns `fallback: true` with `requestedSpecialty.id === 60`.

The collection-level auth is `Bearer {{token}}`, so once login has run every subsequent request inherits the header automatically. The login request itself overrides this with `noauth`.

## Running via Newman (CLI)

Newman is the headless CLI runner Postman ships for CI and command-line smoke checks. Install it once:

```bash
npm install -g newman
```

Then run the whole collection against the booted stack:

```bash
newman run postman/medhead-poc.postman_collection.json \
  -e postman/medhead-poc.postman_environment.json
```

A green run exits `0` and reports zero failed assertions in the per-request summary. Re-run from a cold-seeded database if assertion stability matters:

```bash
docker compose down -v && docker compose up -d postgres osrm
```

CI integration of `newman run` (alongside the rest of the test pipeline) is deliberately deferred to the CI/CD hardening session; this folder is the reusable artefact that pipeline will plug in unchanged.

## Side-effects to be aware of

The main-scenario recommendation request reserves a cardiology bed at Fred Brooks Hospital, decrementing its `available_beds` by one on every successful call (seed value `2`). The fallback request likewise reserves a bed at whichever hospital ends up nearest. After three or four runs the seed will need restoring -- see the [`docker cp` + `psql -f` recipe in `backend/README.md`](../backend/README.md#restoring-the-seed), or the `docker compose down -v` reset above.
