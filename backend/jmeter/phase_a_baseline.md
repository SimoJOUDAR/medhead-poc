# Phase A baseline stress run -- report

> **Session / PR**: S7 / PR-S7-01
> **Plan**: [`baseline.jmx`](./baseline.jmx) + [`baseline.properties`](./baseline.properties)
> **Runner**: [`run-baseline.sh`](./run-baseline.sh)
> **Reference run date**: 2026-04-24, 21:23:24 -- 21:26:53 CEST (3 min 29 s)
> **Committed raw evidence**: [`results.jtl`](./results.jtl) + [`report/index.html`](./report/index.html)
> **Target under test**: the feature-complete backend delivered by the
> preceding back-end implementation session, before any caching or tuning.
> Phase A is the empirical baseline the caching decision is argued against.

---

## Verdict

**FAIL.** The backend misses the SLA on **every dimension**:

- **Read-path throughput ceiling: ~498 req/s aggregate** across the three
  GET endpoints (62% of the 800 req/s target). The `ConstantThroughputTimer`
  was pegged at 800 req/s but the backend could not keep up -- 200 threads
  plateau at a lower rate because every thread waits longer than planned.
- **Read-path p95 is 2.4x -- 4.9x over SLA on every endpoint.** Worst
  offender: `GET /api/v1/hospitals` at **971 ms p95** (771 ms above the
  200 ms target).
- **Write-path p95: 2,489 ms** (2,289 ms above target), and 22% of write-path
  samples returned 503 under 50-thread concurrency -- OSRM saturation,
  independent of latency (see §5 Observations).

Triggers **PR-S7-04** (Caffeine caching) and **PR-S7-05** (Phase B rerun)
per session exit criteria.

---

## 1. Environment

| Field                 | Value                                                      |
|-----------------------|------------------------------------------------------------|
| Host OS               | macOS 26.4.1 (Darwin 25.4.0, build 25E253), arm64          |
| CPU                   | Apple M2 Pro, 10 cores                                     |
| RAM                   | 16 GB                                                      |
| JVM                   | OpenJDK 17.0.7, Zulu 17.42+19-CA (build 17.0.7+7-LTS)      |
| Spring Boot           | 3.5.13 (pinned in `backend/pom.xml`)                       |
| PostgreSQL            | `postgres:16-alpine` (Docker)                              |
| OSRM                  | `osrm/osrm-backend:latest` (Docker; running as linux/amd64 on arm64 host -- Rosetta translation) |
| OSRM graph            | `greater-london-latest.osrm`                               |
| JMeter                | 5.6.3                                                      |
| Postgres JDBC driver  | `postgresql-42.7.4.jar` under `$(brew --prefix jmeter)/libexec/lib/` |
| Backend JVM flags     | `-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100`      |
| Spring profile active | `default` (no caching, no tuning)                          |
| Clock / thermal state | Wall power; machine idle prior to run                      |

## 2. Test plan summary

| Profile                 | Endpoints                                     | Concurrency         | Duration / loops        | Target                                |
|-------------------------|-----------------------------------------------|---------------------|-------------------------|---------------------------------------|
| Read-path steady-state  | `GET /specialties`, `GET /hospitals`, `GET /hospitals/{id}` | 200 threads (30 s ramp) | 120 s measurement       | 800 req/s aggregate, p95 < 200 ms     |
| Write-path burst        | `POST /emergency/recommend`                   | 50 threads (5 s ramp) | 200 iterations / thread | p95 < 200 ms at 50 concurrent users   |

DB state before the write-path profile is reseeded by the JDBC setUp
sampler from `src/main/resources/data.sql` (`INSERT ... ON CONFLICT DO
UPDATE`), so results do not depend on residual local DB state. Seed total:
52 hospital/specialty rows, 170 available beds.

## 3. Results -- read-path steady-state

Source: [`report/statistics.json`](./report/statistics.json) (mirror of the
Statistics tab in [`report/index.html`](./report/index.html)).

| Endpoint                     | Samples | Throughput (req/s) | p50 (ms) | p95 (ms) | p99 (ms) | Error % |
|------------------------------|---------|--------------------|----------|----------|----------|---------|
| `GET /api/v1/specialties`    | 20,056  | 166.3              | 180      | 487      | 838      | 0.00 %  |
| `GET /api/v1/hospitals`      | 20,018  | 166.1              | 364      | 971      | 1,355    | 0.00 %  |
| `GET /api/v1/hospitals/{id}` | 19,940  | 165.5              | 362      | 954      | 1,345    | 0.00 %  |
| **Aggregate (read-path)**    | 60,014  | **497.9**          | --       | **971**  | **1,355** | 0.00 %  |

Throughput target (aggregate): **800 req/s** -- missed by 302.1 req/s
(~38 %). Latency target (per endpoint): **p95 < 200 ms** -- missed on all
three endpoints; every p95 is 2.4x -- 4.9x over target. Zero errors under
load: the backend is not dropping or timing out, it is simply slow under
200-thread pressure.

## 4. Results -- write-path burst

Aggregated sampler view:

| Endpoint                             | Samples | Throughput (req/s) | p50 (ms) | p95 (ms) | p99 (ms) | Error % | Notes |
|--------------------------------------|---------|--------------------|----------|----------|----------|---------|-------|
| `POST /api/v1/emergency/recommend`   | 10,000  | 114.8              | 40       | 2,489    | 3,048    | 98.3 %  | See response-code split below |

Response-code split (from `results.jtl`):

| Code | Count | Share  | Meaning                                                                 |
|------|-------|--------|-------------------------------------------------------------------------|
| 200  | 170   | 1.7 %  | Successful bed reservation. **Equals the seeded bed total** (170) -- every bed reserved exactly once, optimistic lock held (see §6). |
| 404  | 7,632 | 76.3 % | `NO_BEDS_AVAILABLE` after the pool drained. By design: keeps samples latency-meaningful when the happy path is saturated. Fast path (~40 ms p50). |
| 503  | 2,198 | 22.0 % | `routing_unavailable` -- OSRM timed out / disconnected under 50-thread concurrency. Unexpected; flagged in §5.                       |

p50 = 40 ms because the dominant path is the fast 404 reject after the bed
pool drains. p95 = 2,489 ms reflects the minority of samples that either
(a) ran the full recommendation flow while beds were still available, or
(b) sat in the OSRM request path long enough to time out at 2 s. Latency
target **p95 < 200 ms** -- missed by 2,289 ms.

## 5. Observations

- **Read-path has no errors but is bandwidth-capped.** Every GET response
  was 2xx; there are no timeouts, no 401s (token-refresh logic never fired),
  no 5xxs. The 498 req/s ceiling and the 300+ ms average latency together
  say the backend's request-handling path is CPU- or I/O-bound before the
  SLA ceiling. Classic caching-wins shape: the three endpoints serve read
  data that rarely changes under load (specialty catalogue, hospital list,
  hospital detail), so Caffeine on the driven-port adapter should eat most
  of it.
- **2,198 x 503 on the write path is a real finding worth its own line in
  the test-strategy doc.** The backend's OSRM client has a 2 s read
  timeout (`APP_OSRM_READ_TIMEOUT_MS` default). Under 50 concurrent
  recommendation calls, OSRM-local-container latency blew past 2 s often
  enough to produce ~22 % 5xx. OSRM is shipped for arm64-on-amd64 via
  Rosetta on this host, which adds overhead; production would pin a native
  arm64 image or scale OSRM horizontally. An `osrm-distances` cache
  (keyed `roundedLat:roundedLon:hospitalId`) should also collapse most of
  the contention. This 503 rate will be the clearest before/after signal
  in the Phase B rerun.
- **Bed-reservation correctness under concurrency confirmed.** 170 x 200
  exactly equals `SUM(available_beds)` from the seed (52 rows, 170 beds).
  No over-booking, no under-booking, no negative rows (see §6). The
  optimistic-lock path held at 50 threads.
- **Backend is not stable under back-to-back load** (see §7). An earlier
  exploratory run against the same stack showed significantly faster
  numbers (read p95 ~540 ms, aggregate ~658 req/s) -- the reference run
  captured here was the *second* back-to-back invocation and shows
  continuous degradation across the read phase, with throughput falling
  from ~650 req/s to ~17 req/s mid-run. The failure mode is not correctness
  (bed reservations still hold at 170 x 200, no negative rows), it is
  accumulated GC / connection-pool / OSRM-client pressure on a 16 GB host
  that is also running Postgres + OSRM-under-Rosetta + the dev stack.
  Worth flagging because production will see the reference-run shape more
  often than the cold-start shape (each instance serves sustained
  traffic), and it is a second, independent motivation for caching:
  cached reads stop touching the hot dependencies and the degradation
  vector closes.

## 6. DB spot-check

```text
SELECT MIN(available_beds), SUM(available_beds), COUNT(*) FROM hospital_specialties;
--  min | sum | count
-- -----+-----+-------
--    0 |   0 |    52
```

Observed: `MIN = 0` (no row went negative -- optimistic lock enforced
non-negativity under concurrency), `SUM = 0` (every seeded bed was
reserved), `COUNT = 52` (all rows present, reseed landed).

## 7. Stability re-run -- back-to-back degradation

An earlier exploratory run of the same plan against the same (cold) stack,
kicked off 57 minutes before the reference run, produced materially
different numbers. The reference run (committed here) was the *second*
back-to-back invocation. Run 1's raw artefacts were overwritten when the
reference run started (a bug in the initial runner; see
[`run-baseline.sh`](./run-baseline.sh) -- now archives prior outputs under
`./archive/<timestamp>/` instead), so its numbers are preserved textually
only.

| Endpoint                             | p95 run 1 (ms, text) | p95 reference run (ms, committed) | Delta           | Within +/- 10 %? |
|--------------------------------------|----------------------|-----------------------------------|-----------------|------------------|
| `GET /api/v1/specialties`            | 345                  | 487                               | +142 ms (+41 %) | No               |
| `GET /api/v1/hospitals`              | 540                  | 971                               | +431 ms (+80 %) | No               |
| `GET /api/v1/hospitals/{id}`         | 537                  | 954                               | +417 ms (+78 %) | No               |
| `POST /api/v1/emergency/recommend`   | 552                  | 2,489                             | +1,937 ms (+351 %) | No           |

Read-path aggregate throughput also fell from **~658.5 req/s to ~497.9
req/s** (3 x ~166 req/s per endpoint). Run 1's live summary sustained
~650 req/s over its whole read phase; the reference run collapsed to
~17 req/s by the 155 s mark. DB spot-check after the reference run still
clean (`MIN=0, SUM=0, COUNT=52`), so the failure mode is latency /
throughput, not correctness -- same shape both runs, just worse the
second time through.

**What the miss means.** The magnitude (every endpoint +41 % to +351 %,
throughput -24 %) is far beyond any reasonable run-to-run jitter. The
observation is not "noise in the measurement." It is **a real finding:
the backend is not only slow under load (run 1), it is also unstable
under sustained load** (reference run). Run 1's numbers represent the
cold, best-case baseline; the reference run represents what happens when
the same stack is pushed again without a restart. The production
deployment path -- load-balanced instances behind a reverse proxy, each
serving sustained traffic -- will see the reference-run shape, not the
run-1 shape. Noted as a secondary motivation for PR-S7-04: caching
should suppress both the peak-latency miss and the sustained-load
degradation, because the hottest read queries stop hitting the DB and
OSRM stops being re-asked the same route every reconfirmation.

## 8. Reproducing this run

Prerequisites, commands, and interpretation are in [`README.md`](./README.md).
The committed `results.jtl` and `report/` folder are the exact output of
the reference run; regenerating the dashboard from the committed samples:

```bash
cd backend/jmeter
jmeter -g results.jtl -q baseline.properties -o report/
```

Running the full plan again produces a fresh sample set (prior outputs
are archived under `./archive/<timestamp>/` by `run-baseline.sh`).
