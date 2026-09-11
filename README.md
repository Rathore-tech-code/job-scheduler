# Orbit — Distributed Job Scheduler & Workflow Orchestrator

A Quartz/Airflow-style job scheduler: cron-based jobs with DAG dependencies,
concurrency-safe execution across workers, retry with exponential backoff,
a REST + SOAP API, and a live React/TypeScript dashboard.

## What's actually being demonstrated

| Area | Where |
|---|---|
| Algorithms | Min-heap scheduling (`SchedulerEngine`), DFS cycle detection + Kahn's topological sort (`DagService`), lock-free token-bucket rate limiter (`TokenBucketRateLimiter`), exponential backoff with jitter |
| SQL | Flyway-versioned Postgres schema, `SELECT ... FOR UPDATE SKIP LOCKED` for safe concurrent job claiming, composite indexes |
| Spring | Spring Boot 3, Spring Data JPA, Spring Security (JWT), `@Scheduled` |
| REST API | `JobController`, `ExecutionController`, `AuthController` |
| SOAP / Web Services | Spring-WS endpoint at `/soap/jobs` with an XSD-driven auto-generated WSDL |
| TypeScript | React 18 + TS dashboard: typed API client, STOMP/WebSocket live feed, custom SVG DAG layout |

## Architecture

```
 React/TS Dashboard ──REST──▶ Spring Boot API ──┐
        │                                        │
     WebSocket (STOMP)                    SOAP endpoint (/soap/jobs)
        │                                        │
        └────────── Service layer ───────────────┘
                          │
                  SchedulerEngine (min-heap + DAG order)
                          │
                 PostgreSQL (SKIP LOCKED claiming)
```

Every tick, `SchedulerEngine` pops due jobs from an in-memory min-heap,
claims them at the DB level with `SKIP LOCKED` (so running multiple worker
instances never double-executes a job), orders the batch with a topological
sort so dependent jobs wait for their upstream job to succeed, executes with
retry + exponential backoff, then reschedules via the job's cron expression.

## Project structure

```
job-scheduler/
├── backend/                         Spring Boot 3 / Java 17
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/scheduler/
│       │   ├── entity/              Job, JobDependency, Execution, User
│       │   ├── repository/          incl. the SKIP LOCKED native query
│       │   ├── service/              CronPlanner, DagService, SchedulerEngine,
│       │   │                         TokenBucketRateLimiter, JobService, ...
│       │   ├── controller/           REST endpoints
│       │   ├── soap/                 SOAP endpoint + hand-written JAXB model
│       │   ├── security/             JWT filter + util
│       │   ├── config/               Security, WebSocket, SOAP/WSDL, seed data
│       │   ├── dto/ , exception/
│       │   └── SchedulerApplication.java
│       ├── main/resources/
│       │   ├── application.yml, application-test.yml
│       │   ├── db/migration/         Flyway V1__, V2__
│       │   └── xsd/job-service.xsd
│       └── test/java/com/scheduler/
│           ├── service/              DagServiceTest, TokenBucketRateLimiterTest,
│           │                         CronPlannerTest (pure unit tests)
│           └── JobApiIntegrationTest.java  (MockMvc + H2)
└── frontend/                        React 18 + TypeScript + Vite + Tailwind
    └── src/
        ├── api/client.ts            typed fetch wrapper + JWT handling
        ├── hooks/useExecutionStream.ts   STOMP/WebSocket live feed
        ├── types/index.ts
        └── components/
            ├── Dashboard.tsx, LoginScreen.tsx
            ├── JobList.tsx, JobForm.tsx
            ├── DagView.tsx           custom layered SVG dependency graph
            ├── ExecutionFeed.tsx, StatStrip.tsx, StatusBadge.tsx
            └── EmptyState.tsx        empty / error / loading states
```

## Verification status — please read before assuming this is a passing CI run

**Frontend: actually built and verified in this environment.**
`npm install`, `npm run build` (`tsc -b --strict` + `vite build`), and
`eslint` were all run for real here and passed cleanly — see the build log
below. `dist/` was produced successfully.

**Backend: written and manually reviewed, but *not* compiled/tested in this
sandbox.** The build sandbox this project was generated in only has network
access to a fixed allowlist (npm, PyPI, crates.io, GitHub, Ubuntu archives)
that does **not** include Maven Central, so `mvn` cannot download Spring
Boot or any other dependency here — confirmed directly:

```
$ curl -sI https://repo.maven.apache.org/maven2/
HTTP/2 403
x-deny-reason: host_not_allowed
```

To compensate, every file was reviewed by hand: package declarations were
checked against directory structure, every constructor's dependency wiring
was traced, every cross-file method call was checked against its actual
signature, and all XML/YAML config was validated for well-formedness. This
caught two real bugs before you ever see them:

1. `Thread.sleep(Duration)` is a Java 19+ API — the project targets Java 17,
   so this would have failed to compile. Fixed to use `Thread.sleep(long)`.
2. `JwtAuthFilter` was both a `@Component` *and* manually added to the
   Spring Security filter chain — Spring Boot would have additionally
   auto-registered it as a generic servlet filter, running it twice per
   request. Fixed by constructing it manually instead of injecting it.

That said, static review is not the same as a green build. **The first
thing to do is run `mvn clean install` yourself** (see below) — you have
normal internet access and this environment did not. If anything doesn't
compile, it's most likely a small, mechanical fix (an import, a generic
type) rather than a structural problem — the architecture and logic have
been reviewed end-to-end.

### Update: Lombok annotation-processing fix

An earlier version of this project failed to compile locally with errors
like `cannot find symbol: method getId()` on `Job`, `getUsername()` on
`User`, etc. The entity source was correct (`@Getter @Setter
@NoArgsConstructor` were present), but the `pom.xml` left Lombok's version
unpinned and relied on javac's default "scan the classpath for annotation
processors" behavior instead of declaring it explicitly. That default
behavior is well known to be fragile across different local Maven/JDK
combinations — silently applying zero annotation processors instead of
throwing a clear error. This was reproduced and confirmed directly (not
guessed): compiling a Lombok-annotated class with an explicit processor
path that omits Lombok compiles clean but generates no getters/setters,
exactly matching the reported symptom.

Fixed by:
- Pinning an explicit Lombok version (`1.18.46`) instead of an inherited one
- Explicitly declaring `annotationProcessorPaths` in `maven-compiler-plugin`
  so Lombok's processor runs deterministically regardless of environment

This sandbox still can't reach Maven Central to run the full `mvn clean
install` (confirmed again — it now gets past POM parsing and fails only at
downloading `spring-boot-starter-parent`, which is the same network
restriction as before, not a POM error). Please run `mvn clean install`
locally to get the real confirmation.

### Update: PostgreSQL "invalid value for parameter TimeZone: Asia/Calcutta" fix

After `mvn clean install` succeeded, `mvn spring-boot:run` failed connecting
to Postgres with `FATAL: invalid value for parameter "TimeZone":
"Asia/Calcutta"`. Root cause: the Postgres JDBC driver (pgjdbc) always sends
`TimeZone.getDefault().getID()` as a parameter in the connection *startup
packet*. On some JVM/OS/tzdata combinations, the India zone resolves to the
deprecated pre-1996 alias `Asia/Calcutta` instead of the canonical
`Asia/Kolkata`, and PostgreSQL builds linked against current tzdata reject
the alias outright, before any SQL runs.

This was verified against a real local PostgreSQL 16 instance, not just
diagnosed from docs:
- `SELECT * FROM pg_timezone_names WHERE name = 'Asia/Calcutta'` returns
  nothing — this build's tzdata doesn't recognize the alias at all.
- A JDBC connection with the JVM default forced to `Asia/Calcutta`
  reproduced the **exact** reported error.
- Forcing the JVM default to `Asia/Kolkata` *before* connecting fixed it —
  `SHOW TimeZone` then correctly reports `Asia/Kolkata`.
- Two seemingly-reasonable alternatives were tried and **do not work**:
  pgjdbc's `timezone` connection/datasource property, and the
  `options=-c TimeZone=...` URL parameter. Both still produced the same
  FATAL error, because pgjdbc unconditionally uses the JVM default for its
  own auto-added startup parameter regardless of either override. This is a
  currently open pgjdbc bug —
  [pgjdbc/pgjdbc#3642](https://github.com/pgjdbc/pgjdbc/issues/3642) — with
  no connection-string-level workaround as of this writing.

Fixed by:
- `SchedulerApplication.main()` now calls
  `TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))` before
  `SpringApplication.run(...)`, so the correct id is set before Spring ever
  opens a database connection. **This is the only fix that's actually
  effective against this specific error** — confirmed by direct testing.
- `application.yml` and `application-test.yml`: added
  `spring.jpa.properties.hibernate.jdbc.time_zone: Asia/Kolkata` so
  Hibernate's own java.time conversion is explicit and consistent. This is
  unrelated to the startup-packet bug above (it only affects
  conversion/display — every timestamp column is `TIMESTAMPTZ`, storing an
  absolute instant, so this can never change what a stored value means) but
  it's a real gap the same investigation surfaced, so it's fixed too.
- README's `docker run` command: added `-e TZ=Asia/Kolkata -e
  PGTZ=Asia/Kolkata` so the Postgres container's own default timezone GUC
  and logs are consistent with the rest of the stack. This does **not**
  fix the FATAL error by itself (that's a client-side startup-packet value,
  not a server default) but keeps the whole stack coherent.

## Local setup

### Prerequisites
- Java 17+, Maven 3.8+
- Node.js 18+ and npm
- PostgreSQL 14+ running locally (or via Docker)

### 1. Database

```bash
docker run --name job-scheduler-db -e POSTGRES_USER=scheduler \
  -e POSTGRES_PASSWORD=scheduler -e POSTGRES_DB=job_scheduler \
  -e TZ=Asia/Kolkata -e PGTZ=Asia/Kolkata \
  -p 5432:5432 -d postgres:16
```

(Flyway creates the schema automatically on first backend startup — no
manual `CREATE TABLE` needed.)

### 2. Backend

```bash
cd backend
mvn clean install        # run this first — see verification note above
mvn spring-boot:run
```

Runs on `http://localhost:8080`. A default account is seeded on first boot:
**admin / admin123**.

To run just the tests:
```bash
mvn test
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and proxies `/api` and `/ws` to
`localhost:8080` (see `vite.config.ts`). Sign in with `admin` / `admin123`
or register a new account.

For a production build (already verified to succeed):
```bash
npm run build
```

## API surface

**REST**
- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/jobs`, `GET /api/jobs/{id}`, `POST /api/jobs`
- `POST /api/jobs/{id}/trigger`, `PATCH /api/jobs/{id}/status`
- `GET /api/executions`, `GET /api/executions/job/{jobId}`
- WebSocket (STOMP): connect to `/ws`, subscribe to `/topic/executions`

**SOAP** (legacy-client integration surface)
- WSDL: `GET http://localhost:8080/soap/jobs.wsdl`
- Operations: `submitJobRequest` / `submitJobResponse`,
  `getJobStatusRequest` / `getJobStatusResponse`

## Known limitations / next steps

- Backend build is unverified in-sandbox (see above) — verify with
  `mvn clean install` locally as the first step.
- `SchedulerEngine.simulateExecution` fakes real work with a random
  success/failure roll — swap in an actual job executor (HTTP call, shell
  command, message publish) for a real deployment.
- Single JVM instance assumed for the in-memory heap; the DB-level
  `SKIP LOCKED` claim is what actually makes it safe to run multiple
  instances — the heap is just a local optimization to avoid re-polling
  the whole table every second.
- No pagination on `GET /api/jobs` / `GET /api/executions` — fine for a
  portfolio project, would need it at real scale.
- CORS is wide open (`*`) for local development — tighten before deploying
  anywhere public.

## Resume framing

> Built a distributed job scheduling system in Spring Boot with priority-queue-based
> scheduling, DAG-based workflow dependencies (topological sort + cycle detection),
> and concurrency-safe job claiming using row-level DB locking; exposed both REST
> and SOAP (Spring-WS) APIs and a real-time React/TypeScript monitoring dashboard.
