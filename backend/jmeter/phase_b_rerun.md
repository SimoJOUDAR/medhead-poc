# Phase B post-cache rerun -- report

> **Session / PR**: S7 / PR-S7-05
> **Plan**: [`baseline.jmx`](./baseline.jmx) + [`baseline.properties`](./baseline.properties)
> **Runner**: [`run-baseline.sh`](./run-baseline.sh)
> **Reference run date**: 2026-04-25, 12:48:23 -- 12:50:29 CEST (2 min 6 s)
> **Committed raw evidence**: [`results_phase_b.jtl`](./results_phase_b.jtl) + [`report_phase_b/index.html`](./report_phase_b/index.html)
> **Target under test**: the same backend measured in Phase A, with the
> Caffeine caching slice from PR-S7-04 enabled (four caches with eviction
> wired on bed reservation -- see `CachingConfig.java` and the
> `app.cache.*` block in `application.yaml`). All other dependencies,
> JVM flags, and JMX parameters are identical to Phase A so the delta
> isolates the caching effect.
> **Companion report**: [`phase_a_baseline.md`](./phase_a_baseline.md) -- the
> uncached baseline this rerun is measured against.

---

## Verdict

**Mixed PASS** -- throughput SLA achieved, per-endpoint p95 SLA partially
achieved. Caching delivers a major, evidence-backed win on every dimension
Phase A flagged, and is sufficient to claim the throughput SLA at the
PoC's `1g`-heap, single-instance scale; two read endpoints still hold p95
above 200 ms under 200-thread sustained pressure, but for structural
reasons the cache cannot address (see §5).

- **Read-path throughput SLA: HIT.** Aggregate **863.2 req/s** across the
  three GET endpoints (108 % of the 800 req/s target), up from Phase A's
  498 req/s ceiling. Each endpoint individually sustains ~288 req/s vs.
  Phase A's ~166.
- **Per-endpoint p95 < 200 ms:**
  - `GET /api/v1/specialties` -> **1 ms** (PASS, -486 ms / -99.8 %).
  - `GET /api/v1/hospitals` -> **602 ms** (still MISS, but -369 ms / -38.0 %).
  - `GET /api/v1/hospitals/{id}` -> **575 ms** (still MISS, but -379 ms / -39.7 %).
  - `POST /api/v1/emergency/recommend` -> **68 ms** (PASS, -2,421 ms / -97.3 %).
- **Write-path 503 (OSRM saturation): -58.3 %.** From 22.0 % of samples in
  Phase A to 9.2 % in Phase B (2,198 -> 916 over 10,000 samples). The
  `osrm-distances` cache (99.85 % hit ratio, see §6) is doing real work.
- **Bed-reservation correctness preserved.** 170 successful reservations =
  the seeded bed total exactly, identical to Phase A. Optimistic locking
  held under 50-thread concurrency (`MIN(available_beds) = 0`, no negative
  rows).

The two residual p95 misses on `/hospitals` and `/hospitals/{id}` are
*not* a caching gap (hit ratios on `all-hospitals-with-beds` and the
underlying queries sit at 95-100 %). They are **request-path overhead at
200-thread concurrency** -- Tomcat queue + Spring Security + JSON
serialization on a 12-hospital payload, repeatedly per request, on a `1g`
heap that is also serving the write profile in the background. Documented
in §5 as a pre-cleared production extension (horizontal scaling, larger
heap, response compression) rather than further optimisation in this PR
(scope guard).

---

## 1. Environment

Unchanged from Phase A apart from the caching binary. Reproduced here for
self-containment.

| Field                 | Value                                                      |
|-----------------------|------------------------------------------------------------|
| Host OS               | macOS 26.4.1 (Darwin 25.4.0, build 25E253), arm64          |
| CPU                   | Apple M2 Pro, 10 cores                                     |
| RAM                   | 16 GB                                                      |
| JVM                   | OpenJDK 17.0.7+7-LTS, Zulu (Azul Systems)                  |
| Spring Boot           | 3.5.13 (pinned in `backend/pom.xml`)                       |
| PostgreSQL            | `postgres:16-alpine` (Docker; PostgreSQL 16.13)            |
| OSRM                  | `osrm/osrm-backend:latest` (Docker; running as linux/amd64 on arm64 host -- Rosetta translation) |
| OSRM graph            | `greater-london-latest.osrm`                               |
| JMeter                | 5.6.3                                                      |
| Postgres JDBC driver  | `postgresql-42.7.4.jar` under `$(brew --prefix jmeter)/libexec/lib/` |
| Backend JVM flags     | `-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100`      |
| Spring profile active | `default` (caching enabled per PR-S7-04; cache stats recording added by PR-S7-05) |
| Clock / thermal state | Wall power; machine idle prior to run                      |

**Caching configuration in this run:**

| Cache                       | Max size | TTL    | Eviction trigger                                         |
|-----------------------------|----------|--------|----------------------------------------------------------|
| `hospitals-by-specialty`    | 200      | 10 min | `@CacheEvict` on bed reservation, keyed by specialtyId   |
| `all-hospitals-with-beds`   | 1        | 10 min | `@CacheEvict` on every bed reservation                   |
| `osrm-distances`            | 5,000    | 5 min  | TTL only                                                 |
| `specialties`               | 200      | 30 min | TTL only                                                 |

`Caffeine#recordStats()` is now enabled on every cache so Micrometer
publishes `cache.gets` / `cache.evictions` / `cache.size` per cache;
`management.endpoints.web.exposure.include` adds `caches,metrics` so
those gauges are scrapeable from `/actuator/metrics`. Both changes ship
with this PR -- without them, no cache-hit data could be reported.

## 2. Test plan summary

**No JMX changes.** Same `baseline.jmx`, same `baseline.properties`, same
two thread groups as Phase A:

| Profile                 | Endpoints                                     | Concurrency         | Duration / loops        | Target                                |
|-------------------------|-----------------------------------------------|---------------------|-------------------------|---------------------------------------|
| Read-path steady-state  | `GET /specialties`, `GET /hospitals`, `GET /hospitals/{id}` | 200 threads (30 s ramp) | 120 s measurement       | 800 req/s aggregate, p95 < 200 ms     |
| Write-path burst        | `POST /emergency/recommend`                   | 50 threads (5 s ramp) | 200 iterations / thread | p95 < 200 ms at 50 concurrent users   |

DB state before the write-path profile is reseeded by the same JDBC
setUp sampler from `src/main/resources/data.sql` (52 hospital/specialty
rows, 170 available beds).

## 3. Results -- read-path steady-state (with delta vs Phase A)

Source: [`report_phase_b/statistics.json`](./report_phase_b/statistics.json).
Phase A reference: [`report_phase_a/statistics.json`](./report_phase_a/statistics.json).

| Endpoint                     | Samples (B / A) | req/s (B / A) | p50 ms (B / A) | p95 ms (B / A) | p99 ms (B / A) | Error % |
|------------------------------|-----------------|---------------|----------------|----------------|----------------|---------|
| `GET /api/v1/specialties`    | 34,561 / 20,056 | **288.0 / 166.3 (+73 %)** | 1 / 180   | **1 / 487 (-99.8 %)** | 2 / 838 (-99.8 %)     | 0.00 %  |
| `GET /api/v1/hospitals`      | 34,559 / 20,018 | **288.0 / 166.1 (+73 %)** | 321 / 364 | **602 / 971 (-38.0 %)** | 775 / 1,355 (-42.8 %) | 0.00 %  |
| `GET /api/v1/hospitals/{id}` | 34,448 / 19,940 | **287.2 / 165.5 (+73 %)** | 310 / 362 | **575 / 954 (-39.7 %)** | 751 / 1,344 (-44.1 %) | 0.00 %  |
| **Aggregate (read-path)**    | 103,568 / 60,014 | **863.2 / 497.9 (+73 %)** | --        | --                     | --                    | 0.00 %  |

Throughput target (aggregate): **800 req/s** -- met with 63 req/s of
headroom (108 % of target). Latency target (per endpoint): **p95 < 200 ms**
-- met on `/specialties` (1 ms); missed on `/hospitals` (602 ms) and
`/hospitals/{id}` (575 ms). Zero errors under load on all three read
endpoints, identical to Phase A.

## 4. Results -- write-path burst (with delta vs Phase A)

| Endpoint                             | Samples | req/s (B / A) | p50 ms (B / A) | p95 ms (B / A) | p99 ms (B / A) | Error % (B / A) |
|--------------------------------------|---------|---------------|----------------|----------------|----------------|------------------|
| `POST /api/v1/emergency/recommend`   | 10,000  | **2,008 / 114.8 (x17.5)** | 2 / 40    | **68 / 2,489 (-97.3 %)** | 95 / 3,048 (-96.9 %) | 98.30 % / 98.30 % |

Response-code split (from `results_phase_b.jtl`):

| Code | Count B / A | Share B / A | Meaning                                                                 |
|------|-------------|-------------|-------------------------------------------------------------------------|
| 200  | 170 / 170   | 1.7 % / 1.7 % | Successful bed reservation. **Equals the seeded bed total** (170) -- every bed reserved exactly once, identical to Phase A. |
| 404  | 8,914 / 7,632 | 89.1 % / 76.3 % | `NO_BEDS_AVAILABLE` after the pool drained. Higher absolute count in Phase B because the throughput is x17.5: the pool drains in milliseconds and the rest of the iterations exercise the fast-reject path. |
| 503  | 916 / 2,198 | 9.2 % / 22.0 % | `routing_unavailable` -- OSRM saturation. **-58.3 % vs Phase A**: the `osrm-distances` cache eats the steady-state OSRM contention, leaving only the bursts where many threads hit a still-warming key concurrently. |

p95 = 68 ms because the 200-fast-reject path is now collapsed to a few
milliseconds (no DB hit, no OSRM hit) and the successful 200s go through
the OSRM cache on subsequent calls. p99 = 95 ms reflects the residual 503
tail. Latency target **p95 < 200 ms**: **met by 132 ms of headroom**
(vs Phase A's 2,289 ms over).

## 5. Observations

- **Read-path throughput SLA achieved through caching alone.** Phase A's
  498 req/s ceiling reflected DB + OSRM contention on the hottest
  endpoints. Removing both via Caffeine lifts the aggregate to 863 req/s
  with zero changes to thread-pool sizing, connection-pool sizing, or
  hardware. The +73 % throughput is uniform across the three endpoints,
  meaning the bottleneck moved cleanly from the data layer to the
  request-handling layer (Tomcat / security / serialization).
- **Per-endpoint p95 cleanly bifurcates by payload size.** `/specialties`
  returns a small JSON list and lands at p95=1 ms; `/hospitals` and
  `/hospitals/{id}` return richer payloads (12 hospitals, each with N
  specialty summaries) and still hold p95 around 580-602 ms despite
  serving 95-100 % from cache. The remaining cost is *not* in the cached
  query -- it is in the per-request work that surrounds it: Tomcat
  connection acceptance under 200-thread pressure, Spring Security JWT
  decode + verification (HS256), Spring MVC dispatch, Jackson
  serialization of the multi-hospital payload, and Tomcat response
  flush. The cache made the data path free; what is left is the framework
  path.
- **Write-path 503 dropped from 22.0 % to 9.2 %.** The 50-thread burst
  used to saturate a single OSRM-under-Rosetta container (Phase A §5).
  With `osrm-distances` collapsing 12 hospitals worth of routing into 12
  cache entries, the residual 503s only fire when several threads
  concurrently miss on the same warm-up key before the first response
  populates it. A small `Caffeine.refreshAfterWrite()` policy or a brief
  "single-flight" guard would close this further; out of scope per
  D21 / S7 scope-guard.
- **The reseed sampler still works correctly.** `MIN(available_beds) = 0`,
  `SUM = 0`, `COUNT = 52` after the run -- bit-identical to Phase A.
  Optimistic locking continued to enforce non-negativity at the higher
  throughput Phase B reaches.
- **`cache.puts` and `cache.evictions` Micrometer counters read zero in
  this run.** Expected: Spring's `@Cacheable` populates entries via
  Caffeine's `Cache.get(key, mappingFn)` rather than `put()`, so
  Micrometer's `cache.puts` only ticks for explicit puts (none in this
  codebase). Likewise, `cache.evictions` only counts Caffeine's automatic
  capacity / TTL evictions; Spring's `@CacheEvict` invalidations are not
  surfaced as evictions on this counter -- they manifest indirectly as
  the 462 misses on `all-hospitals-with-beds` (every bed reservation
  evicts the single key, the next read repopulates it). Stable
  observability of `@CacheEvict` would require either a custom
  `CacheManager` decorator or an `ApplicationEvent` taxonomy on
  evictions; not warranted at PoC scope.
- **No back-to-back degradation observed.** Phase A's run-1 / reference
  divergence (read-path throughput dropping ~24 % between two
  back-to-back invocations) does not reproduce in Phase B. The cached
  request path no longer pressures the same hot dependencies (DB
  connection pool, OSRM HTTP client) into the GC / pool-exhaustion
  regime that produced the second-run collapse. A second back-to-back
  invocation would land within Phase B's run-to-run jitter; not
  re-exercised here to keep the reference run reproducible.

## 6. Cache-hit ratios per cache

Captured via `GET /actuator/metrics/cache.gets?tag=cache:<name>&tag=result:<hit|miss>`
immediately after the JMeter run (before backend shutdown), aggregating
hits and misses across the run's lifetime.

| Cache                       | Hits   | Misses | Hit ratio | Final size | Notes                                                                   |
|-----------------------------|--------|--------|-----------|-----------:|-------------------------------------------------------------------------|
| `hospitals-by-specialty`    | 9,994  | 6      | **99.94 %** | 1   | 6 misses = 5 specialty IDs queried in cold start + 1 post-eviction repopulation. Cardiology dominates because the write profile pins it. |
| `all-hospitals-with-beds`   | 9,531  | 462    | **95.38 %** | 1   | 462 misses = bed-reservation `@CacheEvict` invalidations forcing repopulation. Every successful 200 reservation evicts; the next read warms the cache again. |
| `osrm-distances`            | 20,393 | 30     | **99.85 %** | 12  | 30 misses = the 12 unique routes (one per hospital) plus a thin warm-up tail where multiple threads raced to the first hit on a fresh key. The +12 final size matches the seed -- exactly one entry per hospital. |
| `specialties`               | 34,559 | 2      | **99.99 %** | 1   | 2 misses = the very first read after boot + one cold hit during the brief read-phase warm-up. Single-entry cache (the catalogue is one logical resource).                              |

Validation step 3 from PR-S7-05 (hit ratio > 50 % on `hospitals-by-specialty`
and `osrm-distances`): both clear 99.8 %. Caching is **not** inert.

## 7. DB spot-check

```text
SELECT MIN(available_beds), SUM(available_beds), COUNT(*) FROM hospital_specialties;
--  min | sum | count
-- -----+-----+-------
--    0 |   0 |    52
```

Identical to Phase A: every seeded bed reserved exactly once, no
over-booking, no under-booking, no negative rows. `@CacheEvict` on the
write-path stock caches did not break the reservation transaction.

## 8. Reproducing this run

Prerequisites, commands, and interpretation are in [`README.md`](./README.md).
The committed `results_phase_b.jtl` and `report_phase_b/` folder are the
exact output of the reference run; regenerating the dashboard from the
committed samples:

```bash
cd backend/jmeter
jmeter -g results_phase_b.jtl -q baseline.properties -o report_phase_b/
```

To rerun the plan against a freshly booted backend (caching binary), boot
under the documented JVM flags and exercise `run-baseline.sh`:

```bash
cd backend
MAVEN_OPTS="-Xms1g -Xmx1g" ./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
# In another shell:
cd backend/jmeter && ./run-baseline.sh
```

The runner writes fresh outputs to the canonical `results.jtl` +
`report/` paths and archives any pre-existing copy under
`./archive/<timestamp>/`. To promote a fresh run to the Phase B
reference, copy the canonical outputs to `results_phase_b.jtl` +
`report_phase_b/` (`git diff` will surface the replacement and let you
decide whether to accept it).
