# Prism REST API — Codebase Guide

Quick orientation for anyone (human or Claude) working in `REST/`. **What to do next: `docs/ROADMAP.md`** (start each session there). Product context: `docs/OVERVIEW.md` (what Prism is, data model today vs target, security model, roadmap). MCP server design (Advisor / Analyst): `docs/MCP.md`. UI how-to: `frontend/docs/DEVELOPER_GUIDE.md`. Read this before opening source files; most questions about structure, endpoints, and conventions are answered here.

_Last updated: 2026-10-27, after step 1c: FROM/JOIN from the metadata, SQL/016 (branch feature-metamodel)._

## Stack

- **Spring Boot 3.5.5**, Java **21**, Maven (wrapper 3.9.11). Artifact: `com.prism:prism-api:1.0.0` → `target/prism-api-1.0.0.jar`.
- **Pure Java** — Groovy and gmavenplus were removed in Oct 2026. No Lombok (only add it if a later change clearly benefits).
- Data access is **raw SQL via `JdbcTemplate`** returning `List<Map<String,Object>>` / `Map<String,Object>`. No JPA entities exist, even though `spring-boot-starter-data-jpa` is on the classpath (`ddl-auto=none`).
- **JSqlParser 5.3** (`com.github.jsqlparser:jsqlparser`) parses DDL for `review_star_schema`; planned for the metric admin screens' expression allow-list too.
- **Spring AI 1.1.x** (`spring-ai-bom`, Spring Boot 3 line) for the MCP server: `spring-ai-starter-mcp-server-webmvc`, annotations in `org.springaicommunity.mcp.annotation` (they move to `org.springframework.ai.mcp.annotation` in Spring AI 2.x).
- PostgreSQL (`jdbc:postgresql://localhost:5432/playpen`, schema `prism`). BCrypt via `spring-security-crypto` (no Spring Security filter chain — endpoints are unauthenticated).
- **New database:** `bash SQL/setup.sh` (creates `playpen` and runs every script in order; fresh databases only). First user: `POST /api/users` (see the root README). Password: set `PRISM_DB_PASSWORD` or `PGPASSWORD` before `./run.sh`.
- Build: `./build.sh` / `build.bat` from `REST/` (runs `npm run build` in `frontend/`, then `mvn clean package`). Run: `./run.sh` (`java -jar target/prism-api-1.0.0.jar`; extra arguments go to Spring Boot, e.g. `./run.sh --prism.mcp.enabled=true`; `./runMCP.sh` does exactly that). `run.sh`/`run.bat` read the database password from the git-ignored `REST/.env.local` (`PGPASSWORD=…`) if present. Windows: `build.bat`, `run.bat`, `runMCP.bat`, `test.bat` (`mvn test`); `REST/.gitattributes` keeps `.bat` files CRLF and `.sh` files LF on every platform (bash scripts such as `SQL/setup.sh` and `tools/parity/run.sh` run from Git Bash).

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
│   ├── mcp/                         MCP server: Prism Advisor (docs/MCP.md); off unless prism.mcp.enabled=true
│   │   ├── AdvisorResources.java    @McpResource: prism://guide/{metric-types,star-schema,drill-down,improvement-paths},
│   │   │                            prism://catalog/metrics (index), prism://catalog/metrics/{id} (one entry)
│   │   ├── AdvisorTools.java        @McpTool draft_metric → MetricDraftService; search_metric_catalog → MetricCatalog;
│   │   │                            review_star_schema → StarSchemaReviewService (timed; never touch a database)
│   │   ├── AdvisorPrompts.java      @McpPrompt choose-metrics(struggle), design-star-schema(description),
│   │   │                            review-schema(ddl): one user message each, from AdvisorPromptTemplates
│   │   ├── AdvisorPromptTemplates.java  prompt list + loader (classpath advisor/prompts/<id>.md, only listed IDs);
│   │   │                            fills {{argument}} placeholders in one pass; required, ≤ 4,000 chars (ddl ≤ 64 KB)
│   │   ├── AdvisorGuides.java       guide list + loader (classpath advisor/guides/<id>.md, only listed IDs)
│   │   ├── MetricCatalog.java       catalog loader (classpath*:advisor/catalog/*.md, YAML header via SnakeYAML),
│   │   │                            index, deterministic word search (problems/name 3, tags/summary 2, body 1)
│   │   ├── CatalogEntry.java        record: one catalog entry (header fields + body + full text)
│   │   ├── MetricDraftService.java  draft_metric rules (type/denominator, limits, forbidden sequences), warnings
│   │   │                            (stored ratios, average of ratios), re-runnable Metrics INSERT, SQL preview via
│   │   │                            the real MetricQueryBuilder
│   │   ├── MetricDraft.java         record {valid, errors, warnings, calculation, insertSql, previewSql}
│   │   ├── StarSchemaReviewService.java  review_star_schema: own statement splitter (quotes, $$, comments), JSqlParser
│   │   │                            per statement (CREATE/ALTER TABLE; others skipped), fact/dimension classification
│   │   │                            (FKs, implied FKs, name hints), rules S0–S8, focus-level suggestions. DDL never
│   │   │                            run, stored or logged (only sizes and counts)
│   │   ├── SchemaReview.java        record {summary, tables, findings, focusAreas, skipped} + nested records
│   │   └── McpConfig.java           local-only filter on /mcp (loopback only, 403 + audit otherwise) — until auth
│   ├── metrics/
│   │   ├── SprintMetricsController.java   /api/metrics (list, data, focus areas/levels)
│   │   ├── SprintMetricsService.java      chart data from IDs: resolves metadata, builds + runs the query
│   │   ├── MetricsMetadataCache.java      in-memory Metrics (with their Facts) + Focus_Levels (with their area's
│   │   │                                  dimension); whole reload every prism.metadata.cache-ttl (15m); unknown
│   │   │                                  ID → one reload (≤ every 10 s); unusable rows skipped with a warning
│   │   ├── MetricQueryBuilder.java        SQL from metadata: calculation by metric type, FROM the metric's fact +
│   │   │                                  JOIN only the dimensions needed (group-by level, filters, dimension
│   │   │                                  aliases in the metric's expressions), columns alias-qualified
│   │   │                                  (sd.year_month); filter values bound (Types.OTHER)
│   │   ├── FactDefinition.java            cached Facts row + its links (Join: dimension, fact col = dim col);
│   │   │                                  refuses two tables of one fact sharing an alias
│   │   ├── TableDefinition.java           schema, table, alias; sqlReference() = "schema"."table" alias
│   │   ├── MetricType.java                percentage | ratio | average | sum (+ default scale)
│   │   ├── MetricDefinition.java          cached Metrics row + its FactDefinition (holds SQL — never sent to the browser)
│   │   ├── MetricInfo.java                API view of a metric (display fields only)
│   │   ├── FocusLevelDefinition.java      cached Focus_Levels row + dimension_pk of its focus area
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
ci/Jenkinsfile                       Jenkins pipeline (Windows agent: build REST, stop and redeploy the jar)
tools/parity/                        metric parity check (README.md): run.sh, fixture/ (hand-checked dataset + expected.tsv),
                                     baseline/demo-baseline.txt (frozen)
src/main/resources/
├── application.properties           incl. per-category log levels (logging.level.<Category>); MCP server settings
├── advisor/guides/*.md              Advisor guides (owner-curated content, served as MCP resources)
├── advisor/catalog/*.md             metric catalog: Markdown + YAML header (id = file name; owner-curated)
├── advisor/prompts/*.md             Advisor prompt templates with {{argument}} placeholders (owner-curated)
├── logback-spring.xml               appenders: console; logs/audit.log + logs/perf.log (daily roll)
└── static/   BUILD OUTPUT of frontend/ (`npm run build`) — do not edit; git-ignored
src/test/java/com/prism/api/
├── ApiApplicationTests.java         @SpringBootTest contextLoads
├── admin/TemporaryPasswordsTests.java     plain JUnit (format, alphabet, randomness)
├── metrics/MetricQueryBuilderTests.java   plain JUnit (SQL per type, row condition, bound filters, guards, FROM/JOIN
│                                          from metadata: only needed joins, aliases in expressions, unlinked level
│                                          refused, name checks, alias clash)
├── metrics/MetricParityTests.java         live-DB parity check, skipped unless -Dprism.parity=true (tools/parity/run.sh);
│                                          + ParityCase (case keys), ParityResults (text format, comparison)
├── mcp/MetricDraftServiceTests.java       plain JUnit (draft_metric rules, warnings, INSERT quoting, preview)
├── mcp/AdvisorGuidesTests.java           plain JUnit (guides load; unknown IDs refused)
├── mcp/MetricCatalogTests.java           plain JUnit (every entry loads, links exist, is a valid Prism metric; search)
├── mcp/StarSchemaReviewServiceTests.java plain JUnit (demo schema passes; each rule S0–S8 triggered; splitter, names)
├── mcp/AdvisorPromptTemplatesTests.java  plain JUnit (placeholders match arguments; only real tools/resources/prompts
│                                          named; limits; argument text never re-substituted)
├── mcp/AdvisorPromptsTests.java          plain JUnit (one user message per prompt; missing argument refused)
└── common/utils/SecurityUtilsTests.java   plain JUnit, no Spring context
src/test/java/com/prism/common/utils/LogUtilsTests.java   plain JUnit (records, masking, cycles, duration)
```

Example chart request body: `docs/chart-request-example.json`.

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
- **`Focus_Areas`** (`focus_area_pk` integer PK, `name`, `column_name` — unused, `sort_order`, `dimension_pk` FK → `Dimensions`, NOT NULL — SQL/015), **`Focus_Levels`** (`focus_level_pk` identity PK, `focus_area_pk` FK, `name` (unique per area), `column_name` = **label** column (the text a bar shows; need not be unique), `key_column_name` = **key** column (group, sort and filter by it; unique per bar, one label per key; NULL = the label column), `drill_to_focus_level_pk` (next level on click; same area via composite FK, no loops via a deferred constraint trigger; NULL = no drill), `sort_order` — **display order only** once the code reads the pointers; SQL/009, SQL/015). **The code doesn't read `dimension_pk`, `key_column_name` or `drill_to_focus_level_pk` yet** (it selects explicit columns, so they're safe); today the UI still drills to the next `sort_order`. Seed (SQL/015): Time has two paths, Year → Year & Quarter → Year & Month → Year & Sprint (new; label `sprint_abbr`, key `sprint_start_date`) and Quarter → Month → Sprint (key `sprint_num`); Organization Level 5 → … → Team.
- **`Metrics`** (SQL/012) — the only place a metric is defined: `metric_pk` (identity PK), `name` (unique), `metric_type` (`percentage`/`ratio`/`average`/`sum`), `numerator_sql` (value for average/sum), `denominator_sql` (percentage/ratio only), `row_condition_sql` (optional, → `FILTER (WHERE …)` on every aggregate), `title`, `unit`, `decimal_places`, `scale_mode` (NULL = type default; `zero_to_100`/`fit`/`fixed` + `scale_min`/`scale_max`), `sort_order`, `is_active`. Legacy `valuecolumnsforselectclause`/`fromclause` are unused. Seeded: Say/Do Ratio, Velocity (avg of `avg_velocity_3_sprints`), Scope Churn (added+removed ÷ GREATEST(committed, completed)), Estimate Changed, % Estimated (both ÷ `total_stories`). The FROM/JOIN is still the demo schema, hard-coded in `MetricQueryBuilder.DEMO_FROM_CLAUSE`.
- **`Users`** (`user_id`, `enterprise_pk`, `email`, `username` (optional; NULL when not given — the API stores blank as NULL, the UI shows the email), `password_hash`, `password_algo`, `password_params` jsonb, `password_updated_at`, `is_active`, `is_temp_password`, `temp_password_expires_at` (SQL/010; NULL once the user picks a password), `failed_login_count`, `locked_until`, `last_login_at`, `created_at`, `updated_at`). Lockout: 5 failed logins → `locked_until = now() + 15 min`.
- **`Enterprises`** (tenant) — `enterprise_pk`, `name`, `url`, `auth_token`, `temp_password_hash` (**unused** — temporary passwords are generated per user; never store a shared default).
- **`System_Settings`** — one row per enterprise: `enterprise_pk` (PK + FK → Enterprises, cascade delete), `temp_password_valid_hours` (default 72, CHECK 1–720), `created_at`, `updated_at`. A trigger on `Enterprises` creates the row for new enterprises (SQL/010).
- All prism tables use quoted CamelCase names (`"prism"."Users"`); quoted names are case-sensitive.
- **Explicit keys vs sequences:** inserting a row with an explicit key into a SERIAL/IDENTITY column does not advance its sequence (007 did this for Enterprises, 002/003 for Sprint_Dim/Team_Dim, Focus_Areas was seeded by hand). SQL/010 moves those sequences past the current max and ties `Focus_Areas_focus_area_pk_seq` to its column. Seed data should omit keys, or a script must `setval` afterwards.
- **CITEXT gotcha:** the Postgres driver returns `citext` columns (`Users.email`, `Users.username`) as `PGobject`, which Jackson serializes as `{"type":"citext","value":…}`. Always select them as `email::text AS email`.
- Confirmed types: `Users.user_id` **UUID**, `Users.email` CITEXT, `Enterprises.enterprise_pk` BIGSERIAL, `focus_area_pk` **integer**.
- DDL lives in `~/Documents/Work/prism/SQL/` (001 star schema + Focus tables, 002–004 demo data, 005 Focus_Areas rebuild, 006 Enterprises, 007 Users, 008 Metrics + Focus_Levels data, 009 sort_order + Focus_Levels PK/FK, 010 System_Settings + key-sequence realignment, 011 blank usernames → NULL, 012 metric types + 5 demo metrics, 013 Data_Sources / Facts / Dimensions / Facts_Dimensions_Assoc + demo seed, 014 `DB_Scripts_Executed`, 015 focus areas → dimensions, focus-level key columns, drill pointers, Year & Sprint level, 016 `Metrics.fact_pk`, 017 repair of 015 for focus areas with another name (links an area to the demo dimension holding all its level columns; drill pointers by dimension)). **The API requires 016 since step 1c** (it reads `Metrics.fact_pk`, `Facts`, `Dimensions`, `Facts_Dimensions_Assoc` and `Focus_Areas.dimension_pk`). **The API requires 009, 010 and 012 to have been run** (013 isn't read yet). **Which scripts ran:** `SELECT * FROM "prism"."DB_Scripts_Executed" ORDER BY name;` (`name` = file name, `date_executed` = last completed; NULL for 001–013, which ran before the table existed).
- **`Data_Sources`, `Facts`, `Dimensions`, `Facts_Dimensions_Assoc`** (SQL/013; **`Facts`, `Dimensions` and `Facts_Dimensions_Assoc` are read by the metadata cache since step 1c**; `Data_Sources` not yet — every query runs on the Prism database's connection until the data-provider seam, step 1d). `Metrics.fact_pk` (SQL/016, NOT NULL) says which fact a metric is calculated from; a focus level whose dimension isn't linked to that fact → 400. `Data_Sources`: `data_source_pk` identity, `enterprise_pk` FK, `name` (unique per enterprise), database columns `db_connection_method` (`prism_database` = the demo schema in the Prism database, no host/credentials; `firewall_allow_list` / `private_link` / `outbound_agent` need `db_host`, `db_name`, `db_credentials_ref`; `db_port` optional) **or** API columns `api_url` (https) + `api_credentials_ref` — exactly one connection (CHECK). `*_credentials_ref` names a secret in the secrets store, never the credentials. `Facts` / `Dimensions`: identity PK, `data_source_pk` FK, `name`, `schema_name`, `table_name` (written quoted; no `"`), `table_alias` (lower-case identifier, e.g. `smf` — what metric expressions use); unique per data source: name, table, alias. `Facts_Dimensions_Assoc`: PK (`fact_pk`, `dimension_pk`), `data_source_pk` with composite FKs so a fact and its dimension share a data source, `fact_column` = `dimension_column` (identifiers). Seed: 'Demo data' (enterprise 'Default') → fact `Sprint_Metrics_Fact smf` joined to `Sprint_Dim sd` (`sprint_pk`) and `Team_Dim td` (`team_pk`): exactly `DEMO_FROM_CLAUSE`. TODO step 3: the cache must refuse a fact and a linked dimension sharing an alias (a constraint can't check across tables).
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
- **Database changes = new migration scripts only.** Add the next numbered file in `prism/SQL/` (`NNN-short_description.sql`, e.g. `009-focus_level_sort_order.sql`). Scripts must be **re-runnable** (`IF NOT EXISTS`, `DO $$ … IF NOT EXISTS … $$`, `ON CONFLICT DO NOTHING`, no unguarded `DROP`). Existing scripts are historical: they may be corrected for accuracy, but they are **not re-executed**, so any fix that must reach the live database needs its own new script. Use quoted CamelCase table names (`"prism"."Focus_Levels"`). **Every script (from 014) ends with its own row in `DB_Scripts_Executed`,** as the very last statement: `INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed) VALUES ('NNN-short_description.sql', now()) ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;`. Run scripts with `psql -v ON_ERROR_STOP=1` (as `setup.sh` does), otherwise psql continues after an error and still records the script.

## Open issues / backlog

1. **SQL injection — metrics** — **closed for the client side (Oct 2026, SQL/012):** `/sprint-metrics` was removed; the UI sends only IDs and values to `/api/metrics/data`, and the server builds every query from cached metadata with bound filter values. Remaining: metric expressions in `Metrics` are trusted SQL (only a `;`/comment guard until the parser exists), and the FROM clause is hard-coded. **Design agreed Oct 2026** (`docs/OVERVIEW.md` §4.3, §6): client sends only IDs/values `{metric_pk, focus_level_pk, filters:[{focus_level_pk, operator:"eq", value}]}`; server builds all SQL from metadata cached in memory (whole cache reloaded every 15 min, configurable; unknown IDs → 400), binds values, later runs on a read-only data-source login. Planned order: (a) ✅ metric types in `Metrics`, cache, ID-based endpoint, UI metric list — next: `Data_Sources`/`Facts`/`Dimensions`/`Facts_Dimensions_Assoc` + focus levels pointing at them, drill pointer `drill_to_focus_level_pk` (feature branch, with the parity harness); (b) read-only connection; (c) admin screens with parse-based validation (JSqlParser allow-list), Test, Preview; (d) schema reading; (e) calculation templates.
2. ~~SQL injection — `fetchFocusLevels`~~ **fixed Oct 2026** (bound int parameter).
3. **400 vs 500**: missing request fields throw `ResourceNotFoundException`/`ParameterValueRequiredException` with no `@ControllerAdvice`, so clients get 500. Add a handler returning 400 (and rename the exception for missing params).
4. ~~`user_id` binding bug~~ **fixed Oct 2026** (all user queries bind a `UUID`; malformed IDs → 400/404).
5. **No authentication** on any endpoint; `change-password` trusts the `X-User-ID` header. Consequences to fix with sessions: the server must take the enterprise from the session (not `?enterprisePk` / `enterprise_pk`), block users with `is_temp_password` from everything except change-password, and refuse self-deactivation. Future: super-users spanning enterprises get an enterprise picker; a list never mixes enterprises.
6. CORS allows `"null"` and `"file://"` origins with credentials (for opening static pages from disk) and only `http://localhost:8080` — revisit before deployment. Test/production URLs are TBD; their origins (or a per-environment property) must be added then. All `localhost` addresses in docs mean a local install.
7. ~~DB credentials committed~~ **fixed Oct 2026**: `spring.datasource.password=${PRISM_DB_PASSWORD:${PGPASSWORD:}}` (environment only; the earlier local dev password is still in git history).
8. "Works identically" verification against a live DB was deferred.
9. ~~`test.bat` jar name is stale~~ **fixed Oct 2026**: `test.bat` runs `mvn test`; `run.bat` matches `run.sh`; `runMCP.bat` added.
10. Frontend: session is just the stored login response (no token); temp-password and enterprise rules are UI-enforced only (see #5). (Filters persisting across focus-area changes is intended — it's how a selection is "rotated" onto another dimension.)
11. **Focus-level sort order** (raised Oct 2026; **design agreed and metadata added 2026-10-24, SQL/015**: label column vs key column per level — to be switched on after step 1c, with an API change: each chart row returns its key and drill-down filters send the key, since labels such as "Sprint 9" repeat across years; a deliberate result change, so a new parity baseline). Original note: chart bars are ordered by the label text, so sprint labels such as "Sprint 10" sort before "Sprint 2", and the same sprint name in different years merges into one bar unless the label includes the year. Focus levels need a sort column separate from the display label (e.g. a sort expression per level, `sprint_start_date` for sprints). Fits the fact/dimension branch, where focus levels point at dimension columns; `review_star_schema` already warns about it (S5).

## Working notes for Claude sessions

- The REST folder lives on the owner's Mac (`~/Documents/Work/prism/REST`); a cloud session reaches it only through the file bridge. **Files nested ≥ 7 folders below `REST`** (e.g. `src/main/java/com/prism/api/common/utils/*`) cannot be *read/staged*, though they can be *written*. Workaround: connect `REST/src` (or deeper) as its own folder.
- The bridge **cannot delete files** — give the owner `rm` commands instead.
- Cloud sessions **cannot reach Maven Central or the npm registry** (403) or the owner's `localhost` Postgres, so `mvn`/`npm install`/`npm run build` must run on the owner's machine. Interim checks used: `javac -Xlint:all -Werror` against API stubs (for `MetricParityTests`: a stub `JdbcTemplate` that runs SQL through `psql`, so the whole harness runs against a local Postgres); for the frontend, `tsc` against type stubs plus an esbuild bundle (real React 19 from the container's global node_modules, shims for router/chart) driven by Playwright with a mocked `/api`.
- The owner builds, runs and tests locally and pastes errors back — prefer asking for the stack trace over re-staging files.
- After editing any file listed above, update this guide's map/endpoints/backlog in the same change — and `docs/OVERVIEW.md` / `frontend/docs/DEVELOPER_GUIDE.md` when product status, flows, conventions or security change.
