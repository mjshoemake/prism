# Prism REST API — Codebase Guide

Quick orientation for anyone (human or Claude) working in `REST/`. **What to do next: `docs/ROADMAP.md`** (start each session there). Product context: `docs/OVERVIEW.md` (what Prism is, data model today vs target, security model, roadmap). MCP server design (Advisor / Analyst): `docs/MCP.md`. UI how-to: `frontend/docs/DEVELOPER_GUIDE.md`. Read this before opening source files; most questions about structure, endpoints, and conventions are answered here.

_Last updated: 2026-10-21, after the permanent metric parity check (feature branch: fact/dimension model)._

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
│   │   ├── UsersService.java        Users SQL, auth, password change, lockout, temp-password expiry
│   │   ├── TemporaryPasswords.java  generate(): random xxxx-xxxx-xxxx temp password (no look-alike chars)
│   │   └── UserActionResult.java    record {success, message?, user?, temporaryPassword?} (@JsonInclude NON_NULL;
│   │                                toString masks the password)
│   ├── auth/
│   │   └── AuthController.java      /api/auth login + change-password
│   ├── common/utils/
│   │   └── SecurityUtils.java       BCrypt hash/verify, BCRYPT_COST = 12
│   ├── metrics/
│   │   ├── SprintMetricsController.java   /api/metrics (list, data, focus areas/levels)
│   │   ├── SprintMetricsService.java      chart data from IDs: resolves metadata, builds + runs the query
│   │   ├── MetricsMetadataCache.java      in-memory Metrics + Focus_Levels; whole reload every
│   │   │                                  prism.metadata.cache-ttl (15m); unknown ID → one reload (≤ every 10 s)
│   │   ├── MetricQueryBuilder.java        SQL from metadata by metric type; filter values bound (Types.OTHER)
│   │   ├── MetricType.java                percentage | ratio | average | sum (+ default scale)
│   │   ├── MetricDefinition.java          cached Metrics row (holds SQL — never sent to the browser)
│   │   ├── MetricInfo.java                API view of a metric (display fields only)
│   │   ├── FocusLevelDefinition.java      cached Focus_Levels row
│   │   ├── ChartDataRequest.java          {metric_pk, focus_level_pk, filters:[{focus_level_pk, operator, value}]}
│   │   └── MetricQuery.java               {sql, parameters}
│   └── settings/
│       ├── SystemSettingsController.java  /api/settings (Admin → System Settings)
│       └── SystemSettingsService.java     System_Settings read/upsert (temp_password_valid_hours, 1–720)
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
tools/parity/                        metric parity check (README.md): run.sh, fixture/ (hand-checked dataset + expected.tsv),
                                     baseline/demo-baseline.txt (frozen); parity.py/Gen.java/cmp.py = retired SQL/012 one-off
src/main/resources/
├── application.properties           incl. per-category log levels (logging.level.<Category>)
├── logback-spring.xml               appenders: console; logs/audit.log + logs/perf.log (daily roll)
└── static/   BUILD OUTPUT of frontend/ (`npm run build`) — do not edit; git-ignored
src/test/java/com/prism/api/
├── ApiApplicationTests.java         @SpringBootTest contextLoads
├── admin/TemporaryPasswordsTests.java     plain JUnit (format, alphabet, randomness)
├── metrics/MetricQueryBuilderTests.java   plain JUnit (SQL per type, row condition, bound filters, guards)
├── metrics/MetricParityTests.java         live-DB parity check, skipped unless -Dprism.parity=true (tools/parity/run.sh);
│                                          + ParityCase (case keys), ParityResults (text format, comparison)
└── common/utils/SecurityUtilsTests.java   plain JUnit, no Spring context
src/test/java/com/prism/common/utils/LogUtilsTests.java   plain JUnit (records, masking, cycles, duration)
```

Example chart request body: `Documentation/SprintMetricsExample.json`.

## Frontend (`REST/frontend/`)

Vite + React 19 + TypeScript SPA; Bootstrap 5 + purple/black theme; Chart.js via react-chartjs-2; React Router (hash routes `/#/metrics`, `/#/users`, `/#/settings`, `/#/change-password`, `/#/login`). **Full layout and conventions: `frontend/README.md`** — read that instead of the source tree.

- `npm run dev` → :5173 with `/api` proxied to :8080. `npm run build` → empties and fills `src/main/resources/static/`; build the frontend before `mvn package`.
- Only `src/api/*Api.ts` call fetch; state + data loading live in feature hooks (`useMetricsExplorer`, `useUsers`); toasts via `useToast`; session via `useAuth` (localStorage key `prism_auth`).
- **No metric-specific information in the UI.** Metrics come from `GET /api/metrics/list` (display fields only); chart requests carry only IDs and values (`filters.ts` → `toRequestFilters`). Value/axis formatting from the metric's unit, decimals and scale mode: `metricFormat.ts`.
- Old single-file app and experiments: `frontend/prototypes/` (not built).

## Endpoints

| Method | Path | Handler → Service | Notes |
|---|---|---|---|
| GET | `/api/metrics/list` | `fetchMetricList()` | active metrics in `sort_order`: `{metric_pk, name, metric_type, title, unit, decimal_places, scale_mode, scale_min, scale_max}` — never SQL; `scale_mode` resolved (type default when NULL) |
| POST | `/api/metrics/data` | `fetchChartData(ChartDataRequest)` | body `{metric_pk, focus_level_pk, filters:[{focus_level_pk, operator:"eq", value}]}` (value null = IS NULL; ≤ 25 filters). 200 `[{label, value}]` grouped/ordered by the level's column; 400 `{success:false,message}` for unknown IDs / operator. SQL built server-side from cached metadata (`MetricQueryBuilder`) |
| GET | `/api/metrics/focus-areas-list` | `fetchFocusAreas()` | `Focus_Areas` ordered by `sort_order` |
| GET | `/api/metrics/focus-levels-list/{focusAreaPK}` | `fetchFocusLevels(int)` | `{focus_level_pk, focus_area_pk, name, column_name}`; pk bound as int (non-numeric → 400); ordered by `sort_order` = drill-down order |
| GET | `/api/users?enterprisePk=N` | `listUsers(long)` | users of one enterprise (active + inactive; UI hides inactive by default). Enterprise comes from the request until auth exists (TODO #5) |
| GET | `/api/users/{userId}` | `getUser()` → `Optional` | 404 if absent or not a UUID |
| POST | `/api/users` | `createUser(enterprisePk, email, username)` | body `{enterprise_pk, email, username}`. Server generates the temporary password: 200 `{success, message, user, temporaryPassword}` (clear text, returned once, `Cache-Control: no-store`); user starts with `is_temp_password = TRUE`, `temp_password_expires_at = now() + valid hours`. 400 bad input / unknown enterprise, 409 duplicate email |
| PUT | `/api/users/{userId}` | `updateUser(...)` | `isActive` null → unchanged (COALESCE); 204; 400 if id not a UUID (same for the other `{userId}` PUT/DELETE) |
| PUT | `/api/users/{userId}/password` | `updatePassword()` | body `{password}`; 204 |
| PUT | `/api/users/{userId}/reset-password` | `resetToTemporaryPassword(userId)` | no body. Generates a temporary password; sets `is_temp_password = TRUE` + expiry, clears lockout; 200 `{success, message, user, temporaryPassword}` (no-store) / 400 bad id or user not found |
| DELETE | `/api/users/{userId}` | `deleteUser()` | 204. **Not used by the UI** — users are deactivated, never deleted (owner decision) |
| GET | `/api/settings?enterprisePk=N` | `getSettings(long)` | `{enterprise_pk, temp_password_valid_hours, updated_at}`; 404 if no row |
| PUT | `/api/settings` | `saveSettings(enterprisePk, hours)` | body `{enterprise_pk, temp_password_valid_hours}` (1–720), upserted; 200/400 `UserActionResult`; audit `SETTINGS_CHANGED` |
| POST | `/api/auth/login` | `authenticateUser(email, pw)` | 200 `{success:true,user}` / 401 `{success:false,message}` (also when a correct temporary password has expired) / 400 if fields empty |
| POST | `/api/auth/change-password` | `changePassword(userId, cur, new)` | header `X-User-ID`; new pw ≥ 8 chars; 400 on failure (incl. expired temporary password); clears `is_temp_password` and the expiry |

## Database objects referenced (schema `prism`)

- **`Sprint_Metrics_Fact`** `smf` — joined to **`Sprint_Dim`** `sd` (`sprint_pk`) and **`Team_Dim`** `td` (`team_pk`). Columns seen in examples: `committed_story_points`, `committed_story_points_completed`, `year_month`, `year_quarter`, `level_2_name`.
- **`Focus_Areas`** (`focus_area_pk` integer PK, `name`, `column_name` — unused so far, `sort_order`), **`Focus_Levels`** (`focus_level_pk` identity PK, `focus_area_pk` FK, `name`, `column_name`, `sort_order` — drill-down order; added by SQL/009). A focus level's `column_name` is what the UI sends as `focusLevelColumn`.
- **`Metrics`** (SQL/012) — the only place a metric is defined: `metric_pk` (identity PK), `name` (unique), `metric_type` (`percentage`/`ratio`/`average`/`sum`), `numerator_sql` (value for average/sum), `denominator_sql` (percentage/ratio only), `row_condition_sql` (optional, → `FILTER (WHERE …)` on every aggregate), `title`, `unit`, `decimal_places`, `scale_mode` (NULL = type default; `zero_to_100`/`fit`/`fixed` + `scale_min`/`scale_max`), `sort_order`, `is_active`. Legacy `valuecolumnsforselectclause`/`fromclause` are unused. Seeded: Say/Do Ratio, Velocity (avg of `avg_velocity_3_sprints`), Scope Churn (added+removed ÷ GREATEST(committed, completed)), Estimate Changed, % Estimated (both ÷ `total_stories`). The FROM/JOIN is still the demo schema, hard-coded in `MetricQueryBuilder.DEMO_FROM_CLAUSE`.
- **`Users`** (`user_id`, `enterprise_pk`, `email`, `username` (optional; NULL when not given — the API stores blank as NULL, the UI shows the email), `password_hash`, `password_algo`, `password_params` jsonb, `password_updated_at`, `is_active`, `is_temp_password`, `temp_password_expires_at` (SQL/010; NULL once the user picks a password), `failed_login_count`, `locked_until`, `last_login_at`, `created_at`, `updated_at`). Lockout: 5 failed logins → `locked_until = now() + 15 min`.
- **`Enterprises`** (tenant) — `enterprise_pk`, `name`, `url`, `auth_token`, `temp_password_hash` (**unused** — temporary passwords are generated per user; never store a shared default).
- **`System_Settings`** — one row per enterprise: `enterprise_pk` (PK + FK → Enterprises, cascade delete), `temp_password_valid_hours` (default 72, CHECK 1–720), `created_at`, `updated_at`. A trigger on `Enterprises` creates the row for new enterprises (SQL/010).
- All prism tables use quoted CamelCase names (`"prism"."Users"`); quoted names are case-sensitive.
- **Explicit keys vs sequences:** inserting a row with an explicit key into a SERIAL/IDENTITY column does not advance its sequence (007 did this for Enterprises, 002/003 for Sprint_Dim/Team_Dim, Focus_Areas was seeded by hand). SQL/010 moves those sequences past the current max and ties `Focus_Areas_focus_area_pk_seq` to its column. Seed data should omit keys, or a script must `setval` afterwards.
- **CITEXT gotcha:** the Postgres driver returns `citext` columns (`Users.email`, `Users.username`) as `PGobject`, which Jackson serializes as `{"type":"citext","value":…}`. Always select them as `email::text AS email`.
- Confirmed types: `Users.user_id` **UUID**, `Users.email` CITEXT, `Enterprises.enterprise_pk` BIGSERIAL, `focus_area_pk` **integer**.
- DDL lives in `~/Documents/Work/prism/SQL/` (001 star schema + Focus tables, 002–004 demo data, 005 Focus_Areas rebuild, 006 Enterprises, 007 Users, 008 Metrics + Focus_Levels data, 009 sort_order + Focus_Levels PK/FK, 010 System_Settings + key-sequence realignment, 011 blank usernames → NULL, 012 metric types + 5 demo metrics). **The API requires 009, 010 and 012 to have been run.**
- **Target data model** (owner, Oct 2026; `docs/OVERVIEW.md` §4.2–4.3): all metadata enterprise-scoped; tables `Data_Sources` (database and API connection columns, exactly one filled) → `Facts` + shared `Dimensions`, linked by the association table `Facts_Dimensions_Assoc` (join columns); metrics belong to a fact; focus areas are defined on dimensions, focus levels are dimension columns; **no Metric Groups** (the fact table plays that role; add later only if needed); opt-out exclusions per metric/focus area/level; dashboards may mix fact tables but offer only focus areas common to all their charts; dashboards → tabs (≤4 charts) → shares; role-based write access to metadata.

## Conventions (agreed)

- Constructor injection, `private final` fields, `public` classes/methods. No Lombok.
- DTOs and value objects are **records**; use `@JsonProperty` where the JSON name isn't a valid/idiomatic Java name (`enterprise_pk`, `isActive`). The JSON contract must not change unintentionally.
- SQL lives in `private static final` **text-block constants** at the top of the service.
- Logging: **SLF4J via category loggers** — never `LoggerFactory` directly. Use `LogCategory.<CAT>.logger(MyClass.class)` (logger name `Category.MyClass`) or `LogCategory.AUDIT.logger()` / `PerformanceLog.time(...)`. Categories: `Application`, `Controller`, `Service`, `SQL`, `ResultSet`, `Core`, `Audit`, `Performance`; `LogUtils.debug(log, obj)` dumps an object's contents; levels per category in `application.properties` (or `--logging.level.SQL=DEBUG` at launch; env vars don't work because logger names are case-sensitive). Only Audit and Performance get their own files (`logs/`, dir = `prism.logging.dir`). Audit messages use `EVENT_NAME key=value` form. **Never log passwords or hashes.**
- **Method tracing:** every method in controllers/services/config starts with `log.debug("Started");` (console pattern prints `Category.Class.method`, so no text beyond "Started" is needed). Every controller handler body is wrapped in `PerformanceLog.time("Class.method", () -> { ... })` so each request's duration goes to `perf.log`. Add both to any new method. Logging internals (`common/logging`), records and exceptions are exempt.
- Groovy-truthiness was preserved as `StringUtils.hasLength(...)` (null/"" rejected, whitespace accepted) and `== null || == 0` for numbers.
- Behaviour changes that aren't strictly required are flagged with `TODO` comments and handled as separate changes.
- **Metric data: store numerator and denominator columns, never percentages.** Ratios are computed in the metric's SELECT as `SUM(numerator) / SUM(denominator)` so they stay correct at every drill-down level (averaging stored percentages = average of averages). See `docs/OVERVIEW.md` §4.1.
- **Calculations must stay exactly right.** Any change that could affect how metric values are calculated (query builder, metric types, metadata, filters) must be proven identical before it ships: run every metric × focus level × filter combination through the old and new paths against the same data and compare exact values (as done for SQL/012: 24,160 cases, 120,580 rows, 0 differences; a deliberate rounding change was detected in 11,792 cases), plus hand-checked expected values for edge cases. **Permanent harness:** `bash tools/parity/run.sh` (hand-checked dataset, all demo-data cases vs the frozen baseline, and a deliberately wrong calculation that must be detected); freeze a new baseline (`--freeze`) only on the owner's machine and only for an agreed change (`tools/parity/README.md`).
- **Database changes = new migration scripts only.** Add the next numbered file in `prism/SQL/` (`NNN-short_description.sql`, e.g. `009-focus_level_sort_order.sql`). Scripts must be **re-runnable** (`IF NOT EXISTS`, `DO $$ … IF NOT EXISTS … $$`, `ON CONFLICT DO NOTHING`, no unguarded `DROP`). Existing scripts are historical: they may be corrected for accuracy, but they are **not re-executed**, so any fix that must reach the live database needs its own new script. Use quoted CamelCase table names (`"prism"."Focus_Levels"`).

## Open issues / backlog

1. **SQL injection — metrics** — **closed for the client side (Oct 2026, SQL/012):** `/sprint-metrics` was removed; the UI sends only IDs and values to `/api/metrics/data`, and the server builds every query from cached metadata with bound filter values. Remaining: metric expressions in `Metrics` are trusted SQL (only a `;`/comment guard until the parser exists), and the FROM clause is hard-coded. **Design agreed Oct 2026** (`docs/OVERVIEW.md` §4.3, §6): client sends only IDs/values `{metric_pk, focus_level_pk, filters:[{focus_level_pk, operator:"eq", value}]}`; server builds all SQL from metadata cached in memory (whole cache reloaded every 15 min, configurable; unknown IDs → 400), binds values, later runs on a read-only data-source login. Planned order: (a) ✅ metric types in `Metrics`, cache, ID-based endpoint, UI metric list — next: `Data_Sources`/`Facts`/`Dimensions`/`Facts_Dimensions_Assoc` + focus levels pointing at them, drill pointer `drill_to_focus_level_pk` (feature branch, with the parity harness); (b) read-only connection; (c) admin screens with parse-based validation (JSqlParser allow-list), Test, Preview; (d) schema reading; (e) calculation templates.
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
- Cloud sessions **cannot reach Maven Central or the npm registry** (403) or the owner's `localhost` Postgres, so `mvn`/`npm install`/`npm run build` must run on the owner's machine. Interim checks used: `javac -Xlint:all -Werror` against API stubs (for `MetricParityTests`: a stub `JdbcTemplate` that runs SQL through `psql`, so the whole harness runs against a local Postgres); for the frontend, `tsc` against type stubs plus an esbuild bundle (real React 19 from the container's global node_modules, shims for router/chart) driven by Playwright with a mocked `/api`.
- The owner builds, runs and tests locally and pastes errors back — prefer asking for the stack trace over re-staging files.
- After editing any file listed above, update this guide's map/endpoints/backlog in the same change — and `docs/OVERVIEW.md` / `frontend/docs/DEVELOPER_GUIDE.md` when product status, flows, conventions or security change.
