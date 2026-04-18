# Non-functional tests -- JMeter baseline (Phase A)

This folder ships the Apache JMeter test plan used to stress the backend
against the non-functional SLA: **p95 < 200 ms, 800 req/s per instance**.
Phase A measures the baseline -- the backend as shipped, with no caching and
no tuning -- so the caching decision taken in subsequent work can be argued
from empirical data, not assertion.

The filled report with percentiles, throughput, verdict, and the
committed raw evidence it cites is in [`phase_a_baseline.md`](./phase_a_baseline.md).
The raw samples (`results.jtl`) and HTML dashboard (`report/`) from the
reference run live next to it so the numbers are independently
verifiable.

## 1. Prerequisites

### 1.1 Apache JMeter 5.6+

```bash
# macOS
brew install jmeter

# Linux
# download the latest 5.6.x .tgz from https://jmeter.apache.org/download_jmeter.cgi,
# extract, and put the bin/ directory on PATH

jmeter --version   # must report 5.6 or newer
```

Phase A deliberately uses the standalone CLI rather than the Maven JMeter
plugin: the stress stack stays outside the build's critical path, so a slow
or flaky load run never blocks the per-PR `./mvnw -B verify` loop.

### 1.2 PostgreSQL JDBC driver on JMeter's classpath

The plan ships a JDBC sampler that reseeds `hospital_specialties.available_beds`
before the write-path burst. JMeter does not bundle a Postgres driver, so
drop the JAR into its `lib/` directory once:

```bash
# macOS (Homebrew)
JMETER_HOME="$(brew --prefix jmeter)/libexec"
curl -L -o "${JMETER_HOME}/lib/postgresql-42.7.4.jar" \
  https://jdbc.postgresql.org/download/postgresql-42.7.4.jar

# Linux tarball install
curl -L -o "${JMETER_HOME}/lib/postgresql-42.7.4.jar" \
  https://jdbc.postgresql.org/download/postgresql-42.7.4.jar
```

Any 42.7.x build matches the `postgres:16-alpine` image declared in
`docker-compose.yml`. Only one driver JAR should live in `lib/` at a time.

### 1.3 Full stack booted

Before launching the run:

- `docker compose up -d postgres osrm` from the repo root.
- OSRM graph pre-processed per the repo-root `readme.md` "Running OSRM
  locally" section. The recommendation endpoint falls over without it.
- Backend started in a separate terminal: `cd backend && ./mvnw spring-boot:run`.
  Confirm `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`.
- Dev credentials `demo` / `demo` must still be the active user (see
  `backend/src/main/resources/application.yaml`); override via
  `user=...` / `password=...` in `baseline.properties` if they have drifted.

### 1.4 JVM flags used for the measured backend run

For the numbers recorded in the Phase A report to be comparable across
machines, the backend should be booted under a steady JVM config:

```bash
cd backend
MAVEN_OPTS="-Xms1g -Xmx1g" ./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

Record the JVM version (`java -version`), host OS/CPU/RAM, and Postgres /
OSRM container versions in the Phase A report's environment block.

## 2. Running the plan

From this folder:

```bash
./run-baseline.sh
```

The runner cleans previous results, invokes JMeter in non-GUI mode against
`baseline.jmx` with parameters from `baseline.properties`, writes the raw
sample log to `results.jtl`, and regenerates the HTML dashboard under
`report/`. On completion it prints the path to `report/index.html`.

To override a single parameter without editing the properties file:

```bash
./run-baseline.sh  # defaults
jmeter -n -t baseline.jmx -q baseline.properties \
  -JreadThroughputPerMin=30000 \
  -l results.jtl -e -o report/
```

## 3. What the plan does

Thread groups run **consecutively** (`TestPlan.serialize_threadgroups=true`)
so each profile has the backend to itself:

1. **setUp -- Reseed hospital_specialties.** One JDBC statement mirroring
   the seed in `src/main/resources/data.sql` (`INSERT ... ON CONFLICT DO
   UPDATE`). Fails fast and stops the test if the DB is unreachable.
2. **setUp -- Initial login.** Calls `POST /api/v1/auth/login`, extracts
   the JWT, and stores it as a shared JMeter property (`token`). Subsequent
   samplers read it from `${__P(token)}`.
3. **Read-path steady-state.** Cycles through
   `GET /api/v1/specialties` -> `GET /api/v1/hospitals` ->
   `GET /api/v1/hospitals/{id}` under a Constant Throughput Timer targeting
   800 req/s aggregate, for `readDuration` seconds, with `readThreads`
   concurrent users. Each sampler carries a JSR223 post-processor that
   refreshes the shared token on a 401 (thread-safe via
   `synchronized(props)`).
4. **Write-path burst.** `writeThreads` concurrent users iterate
   `writeIterations` times each against
   `POST /api/v1/emergency/recommend` with a fixed
   `(specialtyId, latitude, longitude)` triple (defaults: Cardiology at
   Fred Brooks). Once the seeded bed pool drains, subsequent calls exercise
   the fallback path; 404s remain latency-meaningful.

## 4. Results interpretation

Open `report/index.html` after the run. The four panels that matter:

- **APDEX** -- sanity check; sub-0.90 flags a problem even before looking
  at raw percentiles.
- **Statistics** -- per-endpoint rows with p50 / p90 / p95 / p99, throughput
  (req/s), and error %. These are the numbers copied into the Phase A report.
- **Response Times Over Time** -- reveals GC pauses, warm-up artefacts, or
  connection-pool exhaustion as spikes rather than steady rises.
- **Response Time Percentiles** -- the full curve; a long p99 tail that
  lifts p95 above 200 ms is the classic shape of connection-pool or OSRM
  contention.

### 4.1 Verdict rubric

The Phase A report carries a verdict callout sourced from the Statistics
table:

- **PASS** -- every row in the read-path profile holds p95 < 200 ms while
  the aggregate throughput hits `readThroughputPerMin / 60` req/s, AND every
  row in the write-path profile holds p95 < 200 ms at `writeThreads`
  concurrency. Caching work is skipped and documented as a pre-cleared
  production extension.
- **FAIL** -- any of the above misses. The report names the missed metric
  (endpoint, percentile, observed value, gap to target); a Caffeine caching
  slice and a Phase B rerun then follow.

### 4.2 DB spot-check after the write-path profile

```bash
docker exec -e PGPASSWORD=medhead medhead-postgres \
  psql -U medhead -d medhead -tA \
    -c "SELECT MIN(available_beds) FROM hospital_specialties;"
# Expected: >= 0 (the optimistic-lock path never leaves a row negative).
```

Also confirm the reseed landed: a second
`SELECT SUM(available_beds) FROM hospital_specialties;` before the burst
should match the post-run `SUM()` plus the number of successful 200 samples
on `POST /api/v1/emergency/recommend` (modulo any fallback reservations).

### 4.3 Stability check

Re-run the script once. p95 per endpoint should stay within +/- 10 % of the
first run -- a wider swing usually points at thermal throttling, JVM warm-up,
or another process contending for CPU rather than a real backend instability.
Record both runs in the report if they diverge.

## 5. Files

```
backend/jmeter/
├── README.md              <- this file
├── baseline.jmx           <- JMeter test plan
├── baseline.properties    <- externalised parameters
├── run-baseline.sh        <- one-command runner
├── phase_a_baseline.md    <- filled report (numbers + verdict + narrative)
├── results.jtl            <- reference run raw samples (CSV)
├── report/                <- reference run HTML dashboard
└── archive/               <- prior runs preserved locally (git-ignored)
```

`results.jtl` + `report/` are the committed artefacts of the reference
run that `phase_a_baseline.md` cites. A fresh invocation of
`run-baseline.sh` moves the current working-tree copy of both into
`./archive/<timestamp>/` before writing new output, so a local re-run
does not silently overwrite committed evidence -- `git diff` will show
the replacement and lets you decide whether to promote the new run to
the reference.
