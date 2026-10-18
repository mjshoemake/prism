# Prism REST API — Codebase Guide

Quick orientation for anyone (human or Claude) working in `REST/`. Read this before opening source files; most questions about structure, endpoints, and conventions are answered here.

_Last updated: 2026-10-17, after the Groovy → Java migration and the frontend restructure._

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
    │   ├── ResourceNotFoundException.java       (no handler → surfaces as HTTP 500)
    │   └── ParameterValueRequiredException.java (no handler → surfaces as HTTP 500)
    └── logging/
        ├── LogCategory.java         enum of logging categories → SLF4J loggers
        └── PerformanceLog.java      time(op, supplier) → Performance category
src/main/resources/
├── application.properties           incl. per-category log levels (logging.level.<Category>)
├── logback-spring.xml               appenders: console; logs/audit.log + logs/perf.log (daily roll)
└── static/   BUILD OUTPUT of frontend/ (`npm run build`) — do not edit; git-ignored
src/test/java/com/prism/api/
├── ApiApplicationTests.java         @SpringBootTest contextLoads
└── common/utils/SecurityUtilsTests.java   plain JUnit, no Spring context
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
| GET | `/api/metrics/focus-areas-list` | `fetchFocusAreas()` | `Focus_Areas` |
| GET | `/api/metrics/focus-levels-list/{focusAreaPK}` | `fetchFocusLevels(pk)` | pk interpolated into SQL text |
| GET | `/api/users` | `listUsers()` | ordered by `created_at DESC` |
| GET | `/api/users/{userId}` | `getUser()` → `Optional` | 404 if absent (`ResponseEntity.of`) |
| POST | `/api/users` | `createUser(enterprisePk, email, username)` | JSON field `enterprise_pk`; 400 if pk null/0 or email empty; uses enterprise `temp_password_hash`, `is_temp_password = TRUE` |
| PUT | `/api/users/{userId}` | `updateUser(...)` | `isActive` null → unchanged (COALESCE); 204 |
| PUT | `/api/users/{userId}/password` | `updatePassword()` | body `{password}`; 204 |
| PUT | `/api/users/{userId}/reset-password` | `updatePassword()` | body `{newPassword}`; 204 |
| DELETE | `/api/users/{userId}` | `deleteUser()` | 204 |
| POST | `/api/auth/login` | `authenticateUser(email, pw)` | 200 `{success:true,user}` / 401 `{success:false,message}` / 400 if fields empty |
| POST | `/api/auth/change-password` | `changePassword(userId, cur, new)` | header `X-User-ID`; new pw ≥ 8 chars; 400 on failure |

## Database objects referenced (schema `prism`)

- **`Sprint_Metrics_Fact`** `smf` — joined to **`Sprint_Dim`** `sd` (`sprint_pk`) and **`Team_Dim`** `td` (`team_pk`). Columns seen in examples: `committed_story_points`, `committed_story_points_completed`, `year_month`, `year_quarter`, `level_2_name`.
- **`Focus_Areas`** (`focus_area_pk`, `name`), **`Focus_Levels`** (`focus_area_pk`, `name`, `column_name`). A focus level's `column_name` is what the UI sends as `focusLevelColumn`.
- **`Users`** (`user_id`, `enterprise_pk`, `email`, `username`, `password_hash`, `password_algo`, `password_params` jsonb, `password_updated_at`, `is_active`, `is_temp_password`, `failed_login_count`, `locked_until`, `last_login_at`, `created_at`, `updated_at`). Lockout: 5 failed logins → `locked_until = now() + 15 min`.
- **`enterprises`** (lower-case, quoted) — `enterprise_pk`, `temp_password_hash`.
- Column types are **unconfirmed** (notably `user_id`, `focus_area_pk`) — verify against the DB before changing parameter binding.

## Conventions (agreed)

- Constructor injection, `private final` fields, `public` classes/methods. No Lombok.
- DTOs and value objects are **records**; use `@JsonProperty` where the JSON name isn't a valid/idiomatic Java name (`enterprise_pk`, `isActive`). The JSON contract must not change unintentionally.
- SQL lives in `private static final` **text-block constants** at the top of the service.
- Logging: **SLF4J via category loggers** — never `LoggerFactory` directly. Use `LogCategory.<CAT>.logger(MyClass.class)` (logger name `Category.MyClass`) or `LogCategory.AUDIT.logger()` / `PerformanceLog.time(...)`. Categories: `Application`, `Controller`, `Service`, `SQL`, `Audit`, `Performance`; levels per category in `application.properties` (or `--logging.level.SQL=DEBUG` at launch; env vars don't work because logger names are case-sensitive). Only Audit and Performance get their own files (`logs/`, dir = `prism.logging.dir`). Audit messages use `EVENT_NAME key=value` form. **Never log passwords or hashes.**
- Groovy-truthiness was preserved as `StringUtils.hasLength(...)` (null/"" rejected, whitespace accepted) and `== null || == 0` for numbers.
- Behaviour changes that aren't strictly required are flagged with `TODO` comments and handled as separate changes.

## Open issues / backlog

1. **SQL injection — `/sprint-metrics`**: select columns and full WHERE clause come from the client. Needs a design change (owner to provide design detail). Partial mitigation idea: whitelist `focusLevelColumn` against `Focus_Levels.column_name`.
2. **SQL injection — `fetchFocusLevels`**: switch to a bound `?` parameter once `focus_area_pk`'s type is known.
3. **400 vs 500**: missing request fields throw `ResourceNotFoundException`/`ParameterValueRequiredException` with no `@ControllerAdvice`, so clients get 500. Add a handler returning 400 (and rename the exception for missing params).
4. **`user_id` binding inconsistency**: `getUser`/`updateUser`/`deleteUser` bind a String; password paths bind `UUID`. If `user_id` is `uuid`, the String paths likely fail in Postgres.
5. **No authentication** on any endpoint; `change-password` trusts the `X-User-ID` header.
6. CORS allows `"null"` and `"file://"` origins with credentials (for opening static pages from disk) — revisit before deployment.
7. DB credentials are committed in `application.properties`.
8. "Works identically" verification against a live DB was deferred.
9. `test.bat` jar name is stale.
10. Frontend: filters persist across focus-area changes; Users page uses `window.confirm/prompt`; session is just the stored login response (no token).

## Working notes for Claude sessions

- The REST folder lives on the owner's Mac (`~/Documents/Work/prism/REST`); a cloud session reaches it only through the file bridge. **Files nested ≥ 7 folders below `REST`** (e.g. `src/main/java/com/prism/api/common/utils/*`) cannot be *read/staged*, though they can be *written*. Workaround: connect `REST/src` (or deeper) as its own folder.
- The bridge **cannot delete files** — give the owner `rm` commands instead.
- Cloud sessions **cannot reach Maven Central or the npm registry** (403) or the owner's `localhost` Postgres, so `mvn`/`npm install`/`npm run build` must run on the owner's machine. Interim checks used: `javac -Xlint:all -Werror` against API stubs; for the frontend, `tsc` against type stubs plus an esbuild bundle (real React 19 from the container's global node_modules, shims for router/chart) driven by Playwright with a mocked `/api`.
- The owner builds, runs and tests locally and pastes errors back — prefer asking for the stack trace over re-staging files.
- After editing any file listed above, update this guide's map/endpoints/backlog in the same change.
