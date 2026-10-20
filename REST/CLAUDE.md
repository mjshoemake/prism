# Prism REST API — Codebase Guide

Quick orientation for anyone (human or Claude) working in `REST/`. Product context: `docs/OVERVIEW.md` (what Prism is, data model today vs target, security model, roadmap). UI how-to: `frontend/docs/DEVELOPER_GUIDE.md`. Read this before opening source files; most questions about structure, endpoints, and conventions are answered here.

_Last updated: 2026-10-20, after the Groovy → Java migration, frontend restructure, logging categories and LogUtils._

## Stack

- **Spring Boot 3.5.5**, Java **21**, Maven (wrapper 3.9.11). Artifact: `com.prism:prism-api:1.0.0` → `target/prism-api-1.0.0.jar`.
- **Pure Java** — Groovy and gmavenplus were removed in Oct 2026. No Lombok (only add it if a later change clearly benefits).
- Data access is **raw SQL via `JdbcTemplate`** returning `List<Map<String,Object>>` / `Map<String,Object>`. No JPA entities exist, even though `spring-boot-starter-data-jpa` is on the classpath (`ddl-auto=none`).
- PostgreSQL (`jdbc:postgresql://localhost:5432/playpen`, schema `prism`). BCrypt via `spring-security-crypto` (no Spring Security filter chain — endpoints are unauthenticated).
- Build: `./build.sh` / `build.bat` from `REST/` (runs `npm run build` in `frontend/`, then `mvn clean package`). Run: `./run.sh` (`java -jar target/prism-api-1.0.0.jar`). `test.bat` references a stale jar name (`api-0.0.1-SNAPSHOT.jar`).

## Source map

```
src/main/java/com/prism/
├── api/
│   ├── ApiApplication.java          @SpringBootApplication entry point
│   ├── CorsConfig.java              CORS: localhost:8080, "file://", "null"; credentials allowed
│   ├── admin/
│   │   ├── UsersController.java     /api/users CRUD; request DTOs are nested records
│   │   ├── UsersService.java        Users SQL, auth, password change, lockout
│   │   └── UserActionResult.java    record {success, message?, user?} (@JsonInclude NON_NULL)
│   ├── auth/
│   │   └── AuthController.java      /api/auth login + change-password
│   ├── common/utils/
│   │   └── SecurityUtils.java       BCrypt hash/verify, BCRYPT_COST = 12
│   └── metrics/
│       ├── SprintMetricsController.java   /api/metrics
│       ├── SprintMetricsService.java      dynamic STAR-schema queries
│       └── SprintMetricsRequest.java      record {focusLevelColumn, valueColumnsForSelectClause, whereClause}
└── common/
    ├── exceptions/
    │   ├── CoreException.java                   general unchecked wrapper (used by LogUtils)
    │   ├── ResourceNotFoundException.java       (no handler → surfaces as HTTP 500)
    │   └── ParameterValueRequiredException.java (no handler → surfaces as HTTP 500)
    ├── logging/
    │   ├── LogCategory.java         enum of logging categories → SLF4J loggers
    │   ├── LogFrames.java           skip(helperClass): hides logging helpers from Logback %method
    │   └── PerformanceLog.java      time(op, supplier) → Performance category
    └── utils/                       (owner-authored, Oct 2026)
        ├── LogUtils.java            reflection dump of beans/records/Maps/Collections/arrays/XML into log
        │                            lines: debug(logger, obj) / info(logger, obj); dataToStrings(obj).
        │                            Masks sensitive text (password/hash/token…), cycle-safe, no-op when
        │                            the level is off. Tests: src/test/.../common/utils/LogUtilsTests
        └── StringUtils.java         padding, dates, CSV, tokenizing helpers. NOTE: same simple name as
                                     org.springframework.util.StringUtils (used in controllers) — qualify one
src/main/resources/
├── application.properties           incl. per-category log levels (logging.level.<Category>)
├── logback-spring.xml               appenders: console; logs/audit.log + logs/perf.log (daily roll)
└── static/   BUILD OUTPUT of frontend/ (`npm run build`) — do not edit; git-ignored
src/test/java/com/prism/api/
├── ApiApplicationTests.java         @SpringBootTest contextLoads
└── common/utils/SecurityUtilsTests.java   plain JUnit, no Spring context
src/test/java/com/prism/common/utils/LogUtilsTests.java   plain JUnit (records, masking, cycles, duration)
```

Example metrics request body: `Documentation/SprintMetricsExample.json`.

## Frontend (`REST/frontend/`)

Vite + React 19 + TypeScript SPA; Bootstrap 5 + purple/black theme; Chart.js via react-chartjs-2; React Router (hash routes `/#/metrics`, `/#/users`, `/#/change-password`, `/#/login`). **Full layout and conventions: `frontend/README.md`** — read that instead of the source tree.

- `npm run dev` → :5173 with `/api` proxied to :8080. `npm run build` → empties and fills `src/main/resources/static/`; build the frontend before `mvn package`.
- Only `src/api/*Api.ts` call fetch; state + data loading live in feature hooks (`useMetricsExplorer`, `useUsers`); toasts via `useToast`; session via `useAuth` (localStorage key `prism_auth`).
- Metric definitions (incl. the SQL `selectSql` fragment) are in `src/features/metrics/metricDefinitions.ts`; WHERE clause built in `filters.ts`.
- Old single-file app and experiments: `frontend/prototypes/` (not built).

## Endpoints

| Method | Path | Handler → Service | Notes |
|---|---|---|---|
| POST | `/api/metrics/sprint-metrics` | `fetchMetrics(focusLevelColumn, valueColumns, whereClause)` | Body = `SprintMetricsRequest`. Missing/empty field → `ResourceNotFoundException` (500). **Client sends raw SQL fragments.** |
| GET | `/api/metrics/focus-areas-list` | `fetchFocusAreas()` | `Focus_Areas` ordered by `sort_order` |
| GET | `/api/metrics/focus-levels-list/{focusAreaPK}` | `fetchFocusLevels(int)` | pk bound as int (non-numeric → 400); ordered by `sort_order` = drill-down order |
| GET | `/api/users?enterprisePk=N` | `listUsers(long)` | users of one enterprise (active + inactive; UI hides inactive by default). Enterprise comes from the request until auth exists (TODO #5) |
| GET | `/api/users/{userId}` | `getUser()` → `Optional` | 404 if absent or not a UUID |
| POST | `/api/users` | `createUser(enterprisePk, email, username, temporaryPassword)` | body `{enterprise_pk, email, username, temporaryPassword}`; `temporaryPassword` null → enterprise `temp_password_hash`. User starts with `is_temp_password = TRUE`. 400 `{success:false,message}` (bad input / no enterprise default), 409 duplicate email |
| PUT | `/api/users/{userId}` | `updateUser(...)` | `isActive` null → unchanged (COALESCE); 204; 400 if id not a UUID (same for the other `{userId}` PUT/DELETE) |
| PUT | `/api/users/{userId}/password` | `updatePassword()` | body `{password}`; 204 |
| PUT | `/api/users/{userId}/reset-password` | `resetToTemporaryPassword()` | body `{temporaryPassword}` (null → enterprise default); sets `is_temp_password = TRUE`, clears lockout; 200/400 `UserActionResult` |
| DELETE | `/api/users/{userId}` | `deleteUser()` | 204. **Not used by the UI** — users are deactivated, never deleted (owner decision) |
| POST | `/api/auth/login` | `authenticateUser(email, pw)` | 200 `{success:true,user}` / 401 `{success:false,message}` / 400 if fields empty |
| POST | `/api/auth/change-password` | `changePassword(userId, cur, new)` | header `X-User-ID`; new pw ≥ 8 chars; 400 on failure |

## Database objects referenced (schema `prism`)

- **`Sprint_Metrics_Fact`** `smf` — joined to **`Sprint_Dim`** `sd` (`sprint_pk`) and **`Team_Dim`** `td` (`team_pk`). Columns seen in examples: `committed_story_points`, `committed_story_points_completed`, `year_month`, `year_quarter`, `level_2_name`.
- **`Focus_Areas`** (`focus_area_pk` integer PK, `name`, `column_name` — unused so far, `sort_order`), **`Focus_Levels`** (`focus_level_pk` identity PK, `focus_area_pk` FK, `name`, `column_name`, `sort_order` — drill-down order; added by SQL/009). A focus level's `column_name` is what the UI sends as `focusLevelColumn`.
- **`Metrics`** (`metric_pk`, `name`, `valuecolumnsforselectclause`, `fromclause`) — metric SQL pieces; seeded with Say/Do Ratio. **Not yet read by the Java code.**
- **`Users`** (`user_id`, `enterprise_pk`, `email`, `username`, `password_hash`, `password_algo`, `password_params` jsonb, `password_updated_at`, `is_active`, `is_temp_password`, `failed_login_count`, `locked_until`, `last_login_at`, `created_at`, `updated_at`). Lockout: 5 failed logins → `locked_until = now() + 15 min`.
- **`Enterprises`** (tenant) — `enterprise_pk`, `name`, `url`, `auth_token`, `temp_password_hash`. All prism tables use quoted CamelCase names (`"prism"."Users"`); quoted names are case-sensitive.
- **CITEXT gotcha:** the Postgres driver returns `citext` columns (`Users.email`, `Users.username`) as `PGobject`, which Jackson serializes as `{"type":"citext","value":…}`. Always select them as `email::text AS email`.
- Confirmed types: `Users.user_id` **UUID**, `Users.email` CITEXT, `Enterprises.enterprise_pk` BIGSERIAL, `focus_area_pk` **integer**.
- DDL lives in `~/Documents/Work/prism/SQL/` (001 star schema + Focus tables, 002–004 demo data, 005 Focus_Areas rebuild, 006 Enterprises, 007 Users, 008 Metrics + Focus_Levels data, 009 sort_order + Focus_Levels PK/FK). **The API requires 009 to have been run.**
- **Target data model** (owner, Oct 2026): all metadata enterprise-scoped; Metric Groups parent Metrics and Focus Areas; opt-out exclusions per metric/focus area/level; `sort_order` on Focus_Areas/Levels; one metric group per dashboard; dashboards → tabs (≤4 charts) → shares; role-based write access to metadata.

## Conventions (agreed)

- Constructor injection, `private final` fields, `public` classes/methods. No Lombok.
- DTOs and value objects are **records**; use `@JsonProperty` where the JSON name isn't a valid/idiomatic Java name (`enterprise_pk`, `isActive`). The JSON contract must not change unintentionally.
- SQL lives in `private static final` **text-block constants** at the top of the service.
- Logging: **SLF4J via category loggers** — never `LoggerFactory` directly. Use `LogCategory.<CAT>.logger(MyClass.class)` (logger name `Category.MyClass`) or `LogCategory.AUDIT.logger()` / `PerformanceLog.time(...)`. Categories: `Application`, `Controller`, `Service`, `SQL`, `ResultSet`, `Core`, `Audit`, `Performance`; `LogUtils.debug(log, obj)` dumps an object's contents; levels per category in `application.properties` (or `--logging.level.SQL=DEBUG` at launch; env vars don't work because logger names are case-sensitive). Only Audit and Performance get their own files (`logs/`, dir = `prism.logging.dir`). Audit messages use `EVENT_NAME key=value` form. **Never log passwords or hashes.**
- **Method tracing:** every method in controllers/services/config starts with `log.debug("Started");` (console pattern prints `Category.Class.method`, so no text beyond "Started" is needed). Every controller handler body is wrapped in `PerformanceLog.time("Class.method", () -> { ... })` so each request's duration goes to `perf.log`. Add both to any new method. Logging internals (`common/logging`), records and exceptions are exempt.
- Groovy-truthiness was preserved as `StringUtils.hasLength(...)` (null/"" rejected, whitespace accepted) and `== null || == 0` for numbers.
- Behaviour changes that aren't strictly required are flagged with `TODO` comments and handled as separate changes.
- **Metric data: store numerator and denominator columns, never percentages.** Ratios are computed in the metric's SELECT as `SUM(numerator) / SUM(denominator)` so they stay correct at every drill-down level (averaging stored percentages = average of averages). See `docs/OVERVIEW.md` §4.1.
- **Database changes = new migration scripts only.** Add the next numbered file in `prism/SQL/` (`NNN-short_description.sql`, e.g. `009-focus_level_sort_order.sql`). Scripts must be **re-runnable** (`IF NOT EXISTS`, `DO $$ … IF NOT EXISTS … $$`, `ON CONFLICT DO NOTHING`, no unguarded `DROP`). Existing scripts are historical: they may be corrected for accuracy, but they are **not re-executed**, so any fix that must reach the live database needs its own new script. Use quoted CamelCase table names (`"prism"."Focus_Levels"`).

## Open issues / backlog

1. **SQL injection — `/sprint-metrics`**: select columns and full WHERE clause come from the client. Needs a design change (owner to provide design detail). Partial mitigation idea: whitelist `focusLevelColumn` against `Focus_Levels.column_name`.
2. ~~SQL injection — `fetchFocusLevels`~~ **fixed Oct 2026** (bound int parameter).
3. **400 vs 500**: missing request fields throw `ResourceNotFoundException`/`ParameterValueRequiredException` with no `@ControllerAdvice`, so clients get 500. Add a handler returning 400 (and rename the exception for missing params).
4. ~~`user_id` binding bug~~ **fixed Oct 2026** (all user queries bind a `UUID`; malformed IDs → 400/404).
5. **No authentication** on any endpoint; `change-password` trusts the `X-User-ID` header. Consequences to fix with sessions: the server must take the enterprise from the session (not `?enterprisePk` / `enterprise_pk`), block users with `is_temp_password` from everything except change-password, and refuse self-deactivation. Future: super-users spanning enterprises get an enterprise picker; a list never mixes enterprises.
6. CORS allows `"null"` and `"file://"` origins with credentials (for opening static pages from disk) and only `http://localhost:8080` — revisit before deployment. Test/production URLs are TBD; their origins (or a per-environment property) must be added then. All `localhost` addresses in docs mean a local install.
7. DB credentials are committed in `application.properties`.
8. "Works identically" verification against a live DB was deferred.
9. `test.bat` jar name is stale.
10. Frontend: session is just the stored login response (no token); temp-password and enterprise rules are UI-enforced only (see #5). (Filters persisting across focus-area changes is intended — it's how a selection is "rotated" onto another dimension.)

## Working notes for Claude sessions

- The REST folder lives on the owner's Mac (`~/Documents/Work/prism/REST`); a cloud session reaches it only through the file bridge. **Files nested ≥ 7 folders below `REST`** (e.g. `src/main/java/com/prism/api/common/utils/*`) cannot be *read/staged*, though they can be *written*. Workaround: connect `REST/src` (or deeper) as its own folder.
- The bridge **cannot delete files** — give the owner `rm` commands instead.
- Cloud sessions **cannot reach Maven Central or the npm registry** (403) or the owner's `localhost` Postgres, so `mvn`/`npm install`/`npm run build` must run on the owner's machine. Interim checks used: `javac -Xlint:all -Werror` against API stubs; for the frontend, `tsc` against type stubs plus an esbuild bundle (real React 19 from the container's global node_modules, shims for router/chart) driven by Playwright with a mocked `/api`.
- The owner builds, runs and tests locally and pastes errors back — prefer asking for the stack trace over re-staging files.
- After editing any file listed above, update this guide's map/endpoints/backlog in the same change — and `docs/OVERVIEW.md` / `frontend/docs/DEVELOPER_GUIDE.md` when product status, flows, conventions or security change.
