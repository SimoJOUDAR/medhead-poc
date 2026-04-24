# MedHead PoC -- Real-Time Emergency Hospital Bed Allocation

> Proof of Concept for the MedHead Consortium's real-time emergency response system.
> Recommends the nearest hospital with an available bed in the required medical specialty.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
- [Running the Tests](#running-the-tests)
- [CI/CD Pipeline](#cicd-pipeline)
- [Branch workflow](#branch-workflow)
- [Accessibility](#accessibility)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

## Overview

A real-time emergency hospital allocation service for the MedHead Consortium. Given a patient location and a required medical specialty, the API recommends the nearest hospital that has an available bed in that specialty, atomically reserves the bed, and publishes a bed-reservation event. A small React UI consumes the API for demos and manual exercising.

The PoC sits at Layer 2 of the consortium's target architecture (real-time, fault-tolerant, patient-safety critical). It addresses the five risks tracked against the emergency response system: scaling under load, partner-data latency, fallback when the requested specialty is unavailable, sub-200 ms response under load, and external interfaceability via OpenAPI.

## Quick Start

From clone to a running stack (see [Prerequisites](#prerequisites); OSRM needs a [one-off graph preparation](backend/README.md#running-osrm-locally) first):

```bash
git clone <repository-url> && cd medhead-poc

# 1. State + routing (Postgres seeds itself on first backend boot).
#    Set OSRM_GRAPH to the graph you prepared, e.g. the Greater London one:
OSRM_GRAPH=greater-london-latest.osrm docker compose up -d postgres osrm

# 2. Backend on :8080.
cd backend && ./mvnw spring-boot:run

# 3. (separate terminal) Frontend on :5173.
cd frontend && npm ci && npm run dev
```

Open `http://localhost:5173` and log in with `demo` / `demo`, and enter lat+long (eg. latitude `51.523`, longitude `-0.131`). Run the test pyramids with `./mvnw -B verify` (from `backend/`) and `npm test` (from `frontend/`) — neither needs the compose stack running: Testcontainers spins up its own disposable Postgres (Docker daemon required) and WireMock stubs OSRM.

## Architecture

**Hexagonal (ports and adapters).** A framework-free `domain` layer (entities, value objects, ports as interfaces, domain exceptions) is wrapped by an `application` layer (use-case services, DTOs) and an `infrastructure` layer that holds every adapter — Spring MVC controllers, JPA repositories, the OSRM HTTP client, the Spring `ApplicationEvent` publisher, and the Spring Security configuration. Ports are owned by the domain; adapters depend inwards. Swapping Postgres for another store, or `ApplicationEvent` for Kafka, is a one-bean change.

| Layer | Role | Spring annotations |
|---|---|---|
| Domain | Entities, ports, business rules | None |
| Application | Use-case orchestration, transaction boundaries, DTO mapping | `@Service`, `@Transactional` |
| Infrastructure | REST controllers, JPA repositories, OSRM client, event publisher, security config | `@RestController`, `@Repository`, `@Component`, `@Configuration` |

Justifications and trade-offs are spelled out in [`reporting.md`](https://github.com/SimoJOUDAR/medhead-poc-architecture/blob/master/reporting.md) §1, published in the [architecture repository](https://github.com/SimoJOUDAR/medhead-poc-architecture).

## Prerequisites

| Tool | Version | Used by |
|---|---|---|
| JDK (Temurin recommended) | 17 LTS | Backend build / run |
| Maven Wrapper | bundled (`backend/mvnw`) | Backend build / test |
| Node.js | ≥ 20 | Frontend build / test |
| npm | bundled with Node 20 | Frontend dependencies |
| Docker (with Compose v2) | recent | Postgres + OSRM + container-image runs |
| `curl` + `jq` | optional | End-to-end walkthrough below |

JMeter 5.6+ is only required to rerun the stress harness; see [`backend/jmeter/README.md`](backend/jmeter/README.md).

## Running the Application

The full local stack is three processes: Postgres (state), OSRM (road-distance routing), and the Spring Boot backend. The React frontend optionally runs in dev mode on top. The boot sequence is the [Quick Start](#quick-start) above.

The backend listens on `http://localhost:8080`, the frontend on `http://localhost:5173` (Vite proxies `/api/*` to the backend so CORS stays out of the development loop). Health probe: `curl -s localhost:8080/actuator/health` should return `{"status":"UP"}`. The full stack-up walkthrough — including the canonical `§2.6` cardiology scenario — is documented under [End-to-end walkthrough](#end-to-end-walkthrough-the-main-recommendation-scenario) below.

### Backing services and container image

Operational detail lives in [`backend/README.md`](backend/README.md):

- **PostgreSQL** — the `postgres` compose service (`postgres:16-alpine` on `localhost:5432`). On first boot the backend applies `schema.sql` + `data.sql` automatically (12 NHS specialty groups, 80 specialties, 12 fictional UK hospitals, the §2.6 fixture). Ops recipes (logs, stop, volume wipe, [idempotent seed restore](backend/README.md#restoring-the-seed)) in [`backend/README.md`](backend/README.md#running-postgresql-via-docker-compose).
- **OSRM** — road-distance routing. Not required for `./mvnw -B verify` (tests stub the adapter); needed only to exercise `POST /api/v1/emergency/recommend` live. Requires a one-off graph preparation before `docker compose up -d osrm` — see [`backend/README.md`](backend/README.md#running-osrm-locally).
- **Backend container image** — the multi-stage `backend/Dockerfile` (same build the CI `docker` stage smoke-tests): build + run instructions in [`backend/README.md`](backend/README.md#building-the-backend-image).

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

Restore the seed with the [idempotent `docker cp` + `psql -f` recipe](backend/README.md#restoring-the-seed) when you're done experimenting.

## Running the Tests

The test pyramid is reproducible from a clean checkout. Backend uses JUnit 5 + Mockito (unit), Spring Boot Test + Testcontainers + WireMock (integration), and Cucumber + REST Assured (BDD acceptance). Frontend uses Vitest + React Testing Library, plus `jest-axe` for WCAG 2.1 AA.

```bash
# Backend -- unit + integration + Cucumber + JaCoCo aggregate report.
cd backend && ./mvnw -B verify
# Reports: target/surefire-reports/, target/failsafe-reports/,
#          target/site/jacoco-aggregate/index.html

# Frontend -- Vitest + RTL + jest-axe.
cd frontend && npm test
# Coverage variant -- HTML report at frontend/coverage/index.html
cd frontend && npm run test:coverage
```

`./mvnw -B verify` brings up its own disposable Postgres through Testcontainers, so it does **not** need the compose stack running. It also does not need OSRM — the integration tests stub the routing adapter via WireMock.

The detailed pyramid layout, coverage numbers, and Cucumber feature inventory are in [`reporting.md`](https://github.com/SimoJOUDAR/medhead-poc-architecture/blob/master/reporting.md) §4.2 (architecture repository).

### Non-functional tests

Stress coverage lives outside the Maven build so a slow or flaky load run never blocks the per-PR `./mvnw -B verify` loop. The JMeter test plan, its runner script, and the prerequisites checklist (JMeter 5.6+, Postgres JDBC driver on the JMeter classpath, full stack booted) are all documented in [`backend/jmeter/README.md`](backend/jmeter/README.md). One-line reminder:

```bash
cd backend/jmeter && ./run-baseline.sh
```

The run produces an HTML dashboard under `backend/jmeter/report/` whose per-endpoint p50 / p95 / p99 + throughput numbers are the inputs to the verdict callout in each phase report. Two reference runs are committed: [`backend/jmeter/phase_a_baseline.md`](backend/jmeter/phase_a_baseline.md) (uncached baseline) and [`backend/jmeter/phase_b_rerun.md`](backend/jmeter/phase_b_rerun.md) (post-cache rerun); their raw samples (`results_phase_a.jtl`, `results_phase_b.jtl`) and dashboards (`report_phase_a/`, `report_phase_b/`) are committed alongside so the numbers are independently verifiable.

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

The full API surface is exercised by a Newman-runnable Postman collection under [`postman/`](postman/README.md): JWT login (with a token-stash test script populating the `{{token}}` environment variable), the specialty and hospital catalogues, single hospital lookup, and the emergency recommendation endpoint on both the §2.6 main scenario and the fallback path. Every request carries `pm.test` assertions on its status code and at least one shape field so the collection doubles as a self-checking smoke test.

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

Two GitHub Actions workflows gate every change to `master`. Each is split into job-level stages so a failure pinpoints the breaking step and the artefacts come out at the layer that produced them.

### Stages

| Stack | Pipeline |
|-------|----------|
| Backend (`.github/workflows/backend-ci.yml`) | `test` → `build` → `docker` |
| Frontend (`.github/workflows/frontend-ci.yml`) | `lint` ‖ `test` → `build` |

Backend `build` depends on `test`; `docker` depends on `build`. Frontend `lint` runs in parallel with `test`; `build` depends on `test`.

### Per-stage breakdown

**Backend**

| Stage | What runs | Artefacts uploaded |
|-------|-----------|--------------------|
| `test` | `./mvnw -B verify` (Surefire unit + Failsafe integration + Cucumber E2E + JaCoCo aggregate). | `surefire-reports`, `failsafe-reports`, `jacoco-aggregate-report` |
| `build` | `./mvnw -B -DskipTests package`. | `backend-jar` |
| `docker` | Builds `backend/Dockerfile` against the JAR from `build`, boots the container against a Postgres service, asserts `/actuator/health` reports `UP` and `/api/v1/specialties` returns a non-empty list under a JWT. | (container logs streamed to the job summary) |

**Frontend**

| Stage | What runs | Artefacts uploaded |
|-------|-----------|--------------------|
| `lint` | `npm run lint` (ESLint flat config). | -- |
| `test` | `npm run test:coverage` (Vitest + React Testing Library, v8 coverage provider). | `frontend-coverage-report` |
| `build` | `npm run build` (TypeScript project references + Vite production build). | `frontend-dist` |

Artefacts are downloadable from the workflow run page on GitHub: open the run, scroll to the **Artifacts** panel, click the artefact name. They are kept under GitHub's default retention policy.

### Run the same commands locally

```bash
# Backend -- mirrors the three CI stages.
cd backend
./mvnw -B verify
./mvnw -B -DskipTests package
docker build -t medhead-backend:dev .

# Frontend -- mirrors the three CI stages.
cd frontend
npm ci
npm run lint
npm run test:coverage
npm run build
```

The container-boot smoke that the `docker` stage performs is the same flow described under [Building the backend image](backend/README.md#building-the-backend-image), substituting the locally-published Postgres for the workflow's service container.

### Required status checks

Branch protection on `master` requires the six job-level checks below. Direct pushes to `master` are rejected, force-pushes and deletion are blocked, and the branch must be up to date with `master` before merging.

- `backend-ci / test`
- `backend-ci / build`
- `backend-ci / docker`
- `frontend-ci / lint`
- `frontend-ci / test`
- `frontend-ci / build`

See [Branch workflow](#branch-workflow) for the full merge contract.

### Reuse the pipeline as a Solution Building Block

The pipeline is self-contained -- no external SaaS scanners, no shared runner pool, no project-specific scripts outside the listed files. To port it into a sister Spring Boot + Vite codebase, copy:

- `.github/workflows/backend-ci.yml`
- `.github/workflows/frontend-ci.yml`
- `backend/Dockerfile` and `backend/.dockerignore`
- `frontend/package.json` script set (`lint`, `test`, `test:coverage`, `build`) and the matching `eslint.config.js` + `vite.config.ts` `test`/`coverage` blocks.

Conventions to preserve when porting:

- Per-job `actions/setup-node@v4` and `actions/setup-java@v4` with their built-in caches (`cache: npm`, `cache: maven`) and an explicit `cache-dependency-path` for the Node cache.
- Concurrency groups keyed on `${{ github.ref }}` with `cancel-in-progress: true` so superseded runs of the same branch are cancelled automatically.
- `if: always()` on every artefact upload so reports survive a failing test or build stage.
- Job-level required status checks (not workflow-level) so a broken stage names itself in the branch-protection error.

## Branch workflow

The project follows **GitHub Flow**: `master` is the only long-lived branch and is always deployable. Every change lands through a short-lived topic branch and a pull request.

### Branching

- Cut topic branches from `master`; delete them after merge (one branch per PR).
- Branch names use `type/short-description`, kebab-case, where `type` is one of `feat`, `fix`, `ci`, `docs`, `test`, `chore`, `refactor` (for example `ci/backend-workflow`, `feat/bed-availability`).

### Merging to `master`

Direct pushes to `master` are disabled. Every change reaches `master` through a pull request that satisfies all of the following, enforced by branch protection:

- The six job-level status checks listed under [Required status checks](#required-status-checks) are green.
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

The API contract is generated from the running backend by Springdoc and exposed two ways:

| Endpoint | Purpose |
|---|---|
| `GET /v3/api-docs` | OpenAPI 3 JSON spec (machine-readable). |
| `GET /swagger-ui.html` | Browsable Swagger UI (use this to try requests interactively). |

Both endpoints are unauthenticated; the rest of `/api/v1/**` requires a JWT obtained from `POST /api/v1/auth/login` (see [Local dev credentials](#local-dev-credentials)).

The full set of public endpoints is also distributed as a Newman-runnable Postman collection under [`postman/`](postman/README.md), with `pm.test` assertions on each request — useful as a copy-customise SBB for sister teams.

The four routes the PoC implements:

| Method | Path | Authenticated | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | no | Exchange `{username, password}` for a 60-min HS256 JWT. |
| `GET` | `/api/v1/specialties` | yes | List the 80 NHS specialties with their parent group. |
| `GET` | `/api/v1/hospitals` (and `/{id}`) | yes | List hospitals + per-specialty bed availability. |
| `POST` | `/api/v1/emergency/recommend` | yes | Recommend the nearest hospital with an available bed in the requested specialty (the §2.6 main scenario), reserve the bed, publish the bed-reservation event. |

## Project Structure

```
medhead-poc/
├── backend/                       # Spring Boot (Java 17, Maven)
│   ├── src/main/java/com/medhead/poc/
│   │   ├── domain/                # Pure domain (no Spring): models, ports, exceptions
│   │   ├── application/           # Use-case services + DTOs
│   │   └── infrastructure/        # Adapters: web, persistence, routing, event, security
│   ├── src/main/resources/        # application.yaml, schema.sql, data.sql
│   ├── src/test/java/...          # Unit + integration + Cucumber suites
│   ├── src/test/resources/features/   # Gherkin acceptance scenarios
│   ├── jmeter/                    # Stress harness, baseline JMX, Phase A + B reports
│   ├── pom.xml
│   ├── Dockerfile                 # Multi-stage, slim JRE final layer, non-root
│   └── README.md                  # Backend ops: Postgres recipes, image build, OSRM setup
├── frontend/                      # React 19 + TypeScript + Vite
│   ├── src/auth/                  # JWT login, persistence, typed apiClient
│   ├── src/recommend/             # Recommendation form + result card
│   ├── src/__tests__/             # WCAG 2.1 AA via jest-axe
│   ├── package.json               # lint / test / test:coverage / build scripts
│   ├── vite.config.ts             # Dev-proxy to /api + Vitest config
│   └── README.md                  # Frontend scripts, layout, proxy note
├── postman/                       # Newman-runnable collection + environment
├── .github/workflows/             # backend-ci.yml + frontend-ci.yml
├── docker-compose.yml             # postgres:16-alpine + osrm/osrm-backend
└── readme.md
```

The architecture-committee deliverable (`reporting.md`) and the TOGAF architecture documents live in the companion [architecture repository](https://github.com/SimoJOUDAR/medhead-poc-architecture).
