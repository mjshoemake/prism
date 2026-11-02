# Prism — Roadmap & Next Steps

> The single to-do list. Start each work session here. **Design decisions** live in [OVERVIEW.md](OVERVIEW.md)
> (§3.1 hosting, §4.2–4.3 data model, metric types, axis scaling, drill-down); **code facts, conventions
> and the numbered backlog** live in [CLAUDE.md](../CLAUDE.md). This file says *what to do next, in what order*.
> Update it whenever an item is finished or a new one is agreed.

_Last updated: 2026-11-01 (late: STAR schema setup design agreed, next session starts at #5 "Facts")._

---

## 1. In review

> **Owner, 2026-10-31:** items 1–2 (dashboards and the redesign) stay here until the metadata admin screens
> (§2 #5) are built, so an administrator can configure a new STAR schema's facts, dimensions and metrics in the UI.

1. **Saved dashboards** (owner, 2026-10-31; built 2026-10-31): a user creates dashboards with a layout of 1 chart
   (today's page), 2 (one above the other) or 4 (quadrants), and picks the metric for each chart area. Decisions
   (owner): drill-down is dashboard-wide (shared focus area, level and filters: a click filters and drills every
   chart); saved per user; switching to fewer charts keeps the hidden areas' metrics; no tabs for now.
   - SQL/020 `Dashboards` + `Dashboard_Charts`; `/api/dashboards` (owner from `X-User-ID` until #4);
     `useMetricsExplorer` now serves several charts (the Metrics page passes one); Dashboards page (`/#/dashboards`,
     nav Dashboard → Dashboards): pick / create / rename / delete (with confirm), layout switch, a metric picker per
     chart area. A chart whose metric can't use the current level shows the server's message.
   - Checked in the Claude session: SQL rules on Postgres; the API end to end (create, duplicate name 409, other
     user's dashboard 404, hidden areas kept, delete); 42 unit tests; whole-frontend typecheck; a browser run
     (create → 4 charts → pick metrics → one click drills all four → 2 charts → back to 4 restores them → rename →
     delete); the Metrics page's click-through unchanged. No calculation change (parity unaffected).
   - **Owner:** see item 2 (run SQL/019, 020 and 021 together).
2. **Dashboard redesign and per-chart y-axis scale** (owner, 2026-10-31; built 2026-10-31; roadmap #2 done this way):
   - Pages: the Metrics page became **Analysis** (`/#/analysis?dashboard=N`; `/#/metrics` redirects): a dashboard
     picker with an Edit button, the shared focus area / level / filters, and a header label per chart (the metric title,
     "Say/Do Ratio"; "by <level>" dropped 2026-11-01), no metric picker; a user with no dashboards gets "My dashboard" (1 chart, first metric).
     **Dashboards** (`/#/dashboards`) is configuration only: a list (Name, Layout, Metrics; Open, Edit, Delete with
     confirm) and New. The **edit page** (`/#/dashboards/new`, `/#/dashboards/N/edit`): name, layout, a group box per
     visible chart area with its metric and y-axis scale; Save / Cancel return to Analysis (same dashboard) or the
     list; hidden areas are kept. Menu: Dashboard → Analysis, Dashboards.
   - Scale (owner: percentages may expand): metric default, 0–100 (stretches above 100), fit, custom (minimum and/or
     maximum, hard): bars above the maximum get a barber-pole end, bars below a raised minimum a striped stub, the
     tooltip shows the real value, "Axis starts at X" on the chart and the bounds in its header.
   - SQL/021 `Dashboard_Charts.y_axis_mode / y_axis_min / y_axis_max` (+ CHECKs); API: scale on every chart area,
     validated (`validScale`), `GET /api/dashboards/{pk}`.
   - Checked in the Claude session: SQL/021 on a database built from all scripts, run twice, constraints accept and
     refuse the right rows; 15 unit tests (8 new) and an end-to-end run of the controller against Postgres (16
     checks); whole-frontend typecheck against the real React, React Router and Chart.js types; a browser run with
     the real Chart.js (47 checks: auto-create once, edit → Save → back, scale rules and pixels of the barber pole,
     one click drills four charts, validation, hidden areas kept, list New / duplicate name / Edit / Cancel / Delete /
     Open, menu). No calculation change (parity unaffected).
   - **Owner:** `git rm` the five retired files (the build fails until they're gone: they import removed helpers):
     `frontend/src/features/metrics/MetricsPage.tsx`, `frontend/src/features/metrics/components/SelectionsPanel.tsx`,
     `frontend/src/features/metrics/components/ChartPanel.tsx`, `frontend/src/features/dashboards/DashboardsPage.tsx`,
     `frontend/src/features/dashboards/components/DashboardSidebar.tsx`. Run SQL/019, 020 and 021 together (then
     parity: expect only Year & Sprint label differences, and `--freeze`), build, try `/#/analysis` and `/#/dashboards`.
   - Known gap (unchanged): a dashboard offers every focus area, not only those all its metrics share; a chart whose
     metric can't use the chosen level shows the server's message.

3. **Read-only login for metric queries** (roadmap #3; built 2026-10-31):
   - SQL/022 creates `prism_reader` (LOGIN; no superuser, create or replication rights; 20 connections) with
     `default_transaction_read_only`, `statement_timeout 30s`, `lock_timeout 5s`, `idle_in_transaction_session_timeout
     60s`, and `prism.grant_star_tables_to_reader()`: SELECT on exactly the STAR tables registered for
     `prism_database` sources, everything else in those schemas revoked. Password never in a file: set it with
     `ALTER ROLE prism_reader PASSWORD '…'` (setup.sh does it from `PRISM_READER_PASSWORD`). Plain SQL only, so the
     script runs from psql, pgAdmin or any SQL tool (a first version used psql's `\if` and failed in pgAdmin;
     fixed 2026-11-01).
   - API: `MetricQueryConnection` (own Hikari pool `metric-query`, not a DataSource bean; `prism.metric-query.*`,
     password from `PRISM_READER_PASSWORD`; Java-side query timeout 30s). Before the first query it checks the login
     and refuses (503 with the reason) if it can't connect, is Prism's own login, isn't read-only, or can read `Users`
     / `Metrics`; never falls back. `PrismDatabaseDataProvider` runs every chart query on it; timeouts → 503 "took
     longer than 30 s"; each query audited (`METRIC_QUERY …`, no filter values). The parity harness still runs on its
     own connection (`MetricQueryConnection.unchecked`): calculations are unchanged.
   - Checked in the Claude session: SQL/022 run twice, and through setup.sh on two fresh databases (with and without
     a password); as prism_reader: STAR tables readable, `Users` / `Metrics` refused, writes refused (also after
     forcing a read-write transaction), no CREATE, can't re-grant itself, statement timeout. Compiled against the
     real Spring 6.2.10, HikariCP 6.3.2, pgjdbc 42.7.7 jars (from `~/.m2`), 25 unit tests (5 new); a live run of the
     real service through the real pool and driver: 910 metric × level × filter cases (11,795 rows) identical on
     prism_reader and on Prism's own login; wrong password, Prism's login, a login that can read `Users` and a slow
     query each answer 503 with their reason; the grant function removes an extra grant again.
   - **Then (owner, 2026-11-01): STAR tables out of Prism's schema, SQL/023.** Only fact and dimension tables are the
     STAR schema; Prism's metadata must not depend on them. The demo tables moved to schema `demo_star` (ALTER TABLE …
     SET SCHEMA: data, keys, indexes, sequences, generated columns kept); `Facts`/`Dimensions.schema_name` updated;
     the grant function replaced: `prism_reader` gets SELECT on the registered STAR tables and no privilege at all in
     `prism` (not even USAGE; a registered table inside `prism` is refused with a warning). The API's login check now
     works by catalog IDs and also refuses a login that can use `prism`. Migrations 001–019 stay as history: on a new
     database they still create the demo tables in `prism` and 023 moves them. From 023 on, no Prism migration touches
     STAR tables. The Advisor's SQL preview and the tests use `demo_star`; the parity fixture loads into `demo_star`.
   - Checked for 023: no foreign keys, views, triggers or functions between Prism's and the STAR tables (before and
     after); run twice; as prism_reader: `demo_star` readable, nothing in `prism` visible or usable; parity harness
     (the real `MetricParityTests`): all 50,336 demo cases (190,668 rows) identical before and after the move, the
     fixture (built with 023) matches its 18 hand-checked cases, the wrong-calculation check still detected; a new
     database built by setup.sh (001 → 023) has exactly the same schema, grants and data as a migrated one; live API
     run: 910 cases identical before (Prism's login, `prism`) and after (`prism_reader`, `demo_star`); refusals now also
     for a login that can merely use `prism`; 40 unit tests.
   - **Owner:** run SQL/022, then SQL/023 (psql or pgAdmin), then `ALTER ROLE prism_reader PASSWORD '…';`; add
     `PRISM_READER_PASSWORD=…` to
     `REST/.env.local`; build; open Analysis (charts show a 503 message saying what's missing until all is done); run
     parity (`bash tools/parity/run.sh`): expect only the Year & Sprint label differences from 019, then `--freeze`.

Article URLs are added to the Advisor content once the owner's *Agile Metrics* series is
further along (owner, 2026-10-23).

Done: Advisor phase 1 (MCP): content (guides, 15-entry catalog, prompt templates) reviewed by the owner, prompts
verified in the Inspector and each workflow run end to end in an assistant, branches merged (2026-10-24); repository cleanup for public viewing, root README, fresh-database setup script (2026-10-23); parity baseline frozen and committed, retired scripts deleted (2026-10-22: 23,557 cases, 118,168 rows,
0 differences); obsolete files deleted, migrations 010–012 run, Year / Quarter / Month filtering confirmed through
the real JDBC driver (2026-10-21).

## 2. Next build steps (agreed order)

> **Order (owner, 2026-11-01): functionality first.** Next is **#5 (metadata admin screens)**; security (#4) moved
> to "Before production deployment" at the end of this section. Numbers are kept so references stay valid.

1. **Feature branch `feature-metamodel`: fact/dimension model** (created by the owner after the MCP merge,
   2026-10-24; prove-or-fail-fast, merge when confident). Steps, each passing the parity check before the next:
   (a) ✅ SQL/013 tables + demo seed; (b) ✅ SQL/015 focus levels → dimensions, label/key columns, drill pointers
   (#11); (c) ✅ SQL/016–017 + query builder builds FROM/JOIN from the metadata, `DEMO_FROM_CLAUSE` deleted (parity passed on the owner's Mac, 2026-10-30);
   (d) ✅ data-provider seam, drill pointers in the API and UI (parity passed on the owner's Mac, 2026-10-31); (e) ✅ switch on keys (baseline re-frozen 2026-10-31). **Branch complete: merge `feature-metamodel`.**
   - ✅ New tables (SQL/013, 2026-10-24): `Data_Sources` (separate database and API connection columns, exactly one
     filled; `db_connection_method` `prism_database` for the demo schema), `Facts`, `Dimensions`,
     `Facts_Dimensions_Assoc` (join columns; a fact and its dimensions share a data source). Seeded with the demo
     schema; the joins built from the seed match `DEMO_FROM_CLAUSE` row for row (4,056 rows). Not read by code yet.
   - ✅ `DB_Scripts_Executed` (SQL/014, owner's idea, 2026-10-24): one row per completed migration script; every
     script from 014 on ends by recording itself (CLAUDE.md conventions). 013 and 014 run on the owner's database.
   - ✅ Metadata for focus levels (SQL/015, 2026-10-24): `Focus_Areas.dimension_pk`; each level has a **label**
     column (`column_name`, the text a bar shows, need not be unique) and an optional **key** column
     (`key_column_name`: group, sort and filter by it, unique per bar, one label per key; owner, 2026-10-24); drill
     pointers. Not read by the code yet. **Owner:** run SQL/015, run the parity check (expect 0 differing values;
     the new level and changed drill path show as missing / unexpected cases), then `--freeze` and commit the baseline.
   - ✅ Keys switched on (step 1e, 2026-10-31, backlog #11): SQL/018 adds generated label columns to the
     demo `Sprint_Dim` and sets each Time level's label/key (owner: only the sprint key carries the year; Year &
     Quarter "Q4 2024", Year & Month "Dec 2024", Quarter "Q4", Month "Dec", sprints "Sprint 26"). The builder groups,
     sorts and filters by the key; the API returns `{key, label, value}`; the UI sends the key and shows the label.
     Parity harness filters by keys; hand-checked answers updated (Month: Jan/Feb/Apr; Year & Month: Jan 2025 …;
     Sprint filters by number) plus 3 new hand-worked cases (Quarter, Year & Sprint, filter by a key).
     Reviewed in the Claude session on a copy of the owner's database, every metric × level before vs after: **no
     value changes**; labels change on Year & Quarter, Year & Month, Quarter, Month, Year & Sprint; order changes
     only on Year & Sprint and Sprint (now date / number order); Year and Organization levels unchanged. Browser
     check: clicks send keys ("2024 Q4", "2024 12", "2024-12-23"), chips show labels. ✅ On the owner's Mac
     (2026-10-31): before freezing, every difference was a label or sprint-order change with identical values
     (hand-checked 18 cases, 0 differences); baseline re-frozen; then 50,336 cases, 190,668 rows, 0 / 0 / 0.
   - Year & Sprint label with the year at the end (owner, 2026-10-31: across years, "Sprint 26" alone can't be told
     apart): **SQL/019**, label `year_sprint_label` "Sprint 26 2024"; key unchanged, so a label-only change. The
     all-years Sprint level keeps "Sprint 26". **Pending (owner, 2026-10-31): apply 019 with the next database
     change**, then parity (expect differences only in Year & Sprint labels; hand-checked 0) and `--freeze`. Until
     then the demo database shows "Sprint 26" on Year & Sprint; the hand-checked fixture (rebuilt from all scripts each
     run) already uses 019's labels, so `expected.tsv` and `run.sh` stay consistent.
   - ✅ `Metrics` point to a fact (SQL/016 `Metrics.fact_pk`, 2026-10-27); `DEMO_FROM_CLAUSE` deleted. The cache
     loads facts, dimensions and links; the builder writes FROM the metric's fact and JOINs only the dimensions a
     query needs (group-by level, filters, dimension aliases in the metric's expressions), with alias-qualified
     columns. `draft_metric`: preview from an example demo fact; its INSERT fills `fact_pk` from the numerator's
     alias. Checked in the Claude session against local copies of the demo and hand-checked data, old builder vs
     new: 21,050 + 5,320 cases, 0 differences. **Owner:** run SQL/016, build, `bash tools/parity/run.sh` (must be
     0 differing, 0 missing, 0 unexpected against the frozen baseline: the cases don't change).
     First run on the owner's database (2026-10-30): "Unknown focus level: 7", because its second area is named
     "Team Hierarchy" and 015 matched areas by name, so it got no `dimension_pk` and no drill pointers. **SQL/017**
     repairs that by the columns instead of the name (checked on a copy with the renamed area: 13 levels load,
     old vs new 21,050 cases identical). ✅ After 017, parity on the owner's Mac (2026-10-30): 50,336 cases,
     190,685 rows, 0 differing / missing / unexpected; hand-checked dataset 0 differences; both wrong-calculation
     checks detected.
   - **Drill-down pointer** `Focus_Levels.drill_to_focus_level_pk` (✅ metadata, SQL/015). A level without one doesn't
     drill: clicking adds the filter and stays. Checks: same focus area (FK), no loops (trigger). Seed: two Time paths
     (owner, 2026-10-24), Year → Year & Quarter → Year & Month → Year & Sprint, and Quarter → Month → Sprint;
     Organization: Level 5 → … → Team. ✅ Step 1d (2026-10-30): the focus-levels endpoint returns
     `drill_to_focus_level_pk` and the UI drills by it; a level without one only adds the filter ("… doesn't drill
     down further"). Checked: the UI follows Year → … → Year & Sprint, Quarter → Month → Sprint, Level 5 → … → Team.
   - ✅ Data-provider seam (step 1d, 2026-10-30): `MetricDataProvider` (source-neutral: data source, metric, level,
     filters), `MetricDataProviders` (by connection method; none → 400), `PrismDatabaseDataProvider`
     (`prism_database`). Checked in the Claude session: 300 random cases through the real service vs the original
     builder, identical. ✅ Parity on the owner's Mac (2026-10-31): 50,336 cases, 0 differing / missing / unexpected.
   - Server reaches data through one "metric data provider" seam (database now, client API later).
   - **Parity first:** ✅ permanent check built (2026-10-21): `MetricParityTests` through the real service, a
     hand-checked dataset (zero denominators, NULL values and labels, empty groups, apostrophes, ties, rounding,
     the same team in two months, the Team A/B 38%-vs-57.5% example), the demo data vs a frozen baseline, and a
     wrong-calculation check. First baseline frozen 2026-10-22 (23,557 cases, 0 differences). Every later step on this
     branch must pass `bash tools/parity/run.sh` with 0 differences.
2. ~~**Y-axis Scale control**~~ **built 2026-10-31 as a per-chart setting on the dashboard edit page** (§1 item 2;
   SQL/021). A metric's own `fixed` scale stays a suggestion; only a chart's custom bounds are hard.
3. ~~**Read-only database login** for metric queries~~ **built 2026-10-31** (§1 item 3; SQL/022). Client databases
   (other connection methods) still need their own provider: credentials from the secrets store, TLS, the same
   login checks.
5. **Metadata admin screens** (owner, 2026-10-31: configuring Prism for a new database must be possible in the UI,
   without knowing Prism's internal tables; metrics admins only and enterprise from the session once #4 exists;
   built now without those checks (owner, 2026-11-01: functionality first), role checks added with #4; they go
   under a new **Metrics** menu, details to come from the owner). Also fold in backlog **#3** (missing request
   fields → 400 via `@ControllerAdvice` instead of 500): a functional fix these screens need:
   - ✅ **Data sources** (built 2026-11-01; owner: the data source is the parent of everything else, under the
     enterprise, so it comes first): Metrics → Data Sources list (fact / dimension / metric counts, Test, Edit,
     Delete refused while facts or dimensions belong to it) and edit page (database: how Prism connects, and for
     client databases host, port, database, the secret's NAME; client API: https URL + secret name; never
     credentials). **Test connection** runs the read-only login check and reads each registered table for the Prism
     database; other connections are saved but answered honestly ("can't connect yet"). `/api/data-sources`. Checked:
     47 unit tests (7 new), a real Spring context against Postgres (15 checks: counts, all three demo tables readable,
     a revoked grant reported, create / duplicate any case / validation / unknown enterprise, API switch clears
     database fields, other enterprise 404, delete 409 with counts then 204), browser run (19 checks + screenshots).
     Also fixed: Analysis could send "create My dashboard" twice when the page mounted twice in a row (the server
     refused the second, so no duplicate); the guard is now shared across mounts. **Next: facts and dimensions**
     (under a data source).
   - **Owner, 2026-11-01:** facts, dimensions and the rest are managed **inside each data source** (they only exist
     in the context of one), not as separate Metrics menu pages: the data source's page lists its facts and
     dimensions (then focus areas, metrics) and adds / edits them there.
   - ✅ **Test connection reports what the login can read** (owner, 2026-11-01: "more useful than reporting that
     nothing has been registered yet"): tables/views the connection's login may SELECT, per schema (system schemas
     left out), on the data source's own connection; a login that can read nothing fails the test. Registered
     tables are still checked one by one. Username field: no placeholder (a grey "prism_reader" looked typed in);
     missing username / password shown under the field.
   - **STAR schema setup: design agreed with the owner, 2026-11-01** (build next, in a new session):
     - **Tables list** on the data source's page, once the connection works: every table the connection can read,
       as a table with columns **Schema, Table, Type, Status** (plus the alias). **Type:** Fact, Dimension or
       **Not Used** (owner: "needs to be added for sure": staging, lookup or audit tables are neither). **Status**
       is worked out by Prism, never set by hand: **Configured** (green) or **Not Configured** (red); also red
       **Missing** for a registered table the connection can no longer see (renamed / dropped).
     - **Status rolls up, "like red/green on a submarine"** (owner): nothing is green while something under it has
       a hole.
       - A **dimension** is Configured only when it has its focus area and every one of its columns is mapped to a
         focus level or marked Not Used (details deferred: see focus levels below).
       - A **fact** is Configured only when it has at least one dimension, each association has its join columns,
         and **every associated dimension is Configured** (green) itself.
       - Not Used tables count as done; the data source is complete when every table has a type and every fact
         and dimension is green.
     - **Fact editor** (the next build step): edit a fact table and choose its dimensions with **two list boxes,
       Available Dimensions and Selected Dimensions**, moved across with left / right arrow buttons, and the
       selected ones **reordered with up / down arrow buttons** (owner). Keyboard (proposed): Up / Down move the
       selection, Right / Left move the selected dimension across, Alt+Up / Alt+Down reorder (plain Up / Down
       must still move through a long list), double-click moves across.
       - **Join columns per association** (Facts_Dimensions_Assoc needs them: `fact_column = dimension_column`):
         each selected dimension shows its join ("on sprint_pk = sprint_pk"), suggested by Prism from foreign keys,
         else from matching column names (right for every demo join), editable.
       - **Alias** per fact / dimension (what metric expressions use: smf, sd, td): suggested from the table name's
         initials, editable, unique within a fact and its dimensions.
       - **Order** of the selected dimensions: needs a new column (Facts_Dimensions_Assoc has none; a new SQL
         script). Proposed meaning: the order of that fact's focus areas wherever they're listed (e.g. Analysis
         sidebar); owner didn't object, confirm when building.
       - Guards (409 with what uses it): a fact with metrics can't change type; a dimension a metric's focus area
         depends on can't be removed from its fact.
       - prism_database sources: run `prism.grant_star_tables_to_reader()` after saving.
     - **Needs first:** schema reading (#6): the connection's tables with their columns, types and foreign keys
       (catalog queries on the data source's own connection, like Test connection's count).
     - **Focus areas and levels: deferred** (owner: "Let's deal with focus levels when we get there. We'll need to
       look at an actual dimension table with associated focus area/levels and walk through the mapping to ensure
       it makes sense against our data model. I don't think that's going to be a quick thing."). Points raised to
       walk through then: a focus level uses a **key** column and a **label** column (e.g. `year_month` /
       `year_month_label`), so a column maps to a level as its key or its label; join-key columns (e.g.
       `sprint_pk`) could count as mapped automatically; a dimension's levels have a display order and **drill
       paths** (the demo Time area has two: Year → Quarter → Month → Sprint, and Year → Year & Sprint).
     - **Build order:** (1) schema reading; (2) tables list with Type (Fact / Dimension / Not Used), alias and
       status; (3) fact editor (two lists, join columns, order, keyboard); then, after the walkthrough, (4) the
       dimension editor (focus area, column → level mapping, Not Used columns, level order and drill paths);
       (5) metrics with the live preview.
   - ✅ **TEMPORARY development-only connection** (built 2026-11-01, owner: "a fourth purely-temporary option …
     where the secret is entered and stored … while we build out the remaining functionality. We just remove the
     option later"; then "a table of key value pairs associated [with] a datasource. That's more flexible"):
     method **Direct (development only)** (`direct_dev`, SQL/024) with host, port, database and a username +
     password kept in the new **`Data_Source_Properties`** table (name/value per data source; the password marked
     secret: never returned, blank keeps it). Own pool per data source, the same fail-closed login check as the
     metric login (read-only, not a superuser, no access to `prism`; never Prism's login), rebuilt when the settings
     change; Test connection and charts both use it; only allow-listed settings reach the driver. On while
     `prism.data-sources.allow-dev-connections=true` (application.properties). Checked: unit tests (7 new), a real
     Spring context against Postgres (27 checks: a direct_dev source → localhost/playpen as `prism_reader`, the demo
     source switched to direct_dev gave **identical results for all 65 metric × level charts (1,300 rows)**, Prism's
     login and a superuser refused, wrong password named, password never returned, pool rebuilt on change, turned
     off → refused), browser runs (14 new checks).
     **To remove it** (when the secrets store exists): delete or convert the `direct_dev` sources; drop
     `direct_dev` from the two Data_Sources CHECKs (a new script); delete secret rows from `Data_Source_Properties`
     (the table stays for non-secret settings such as `sslmode`); remove `ClientDatabaseDataProvider`'s method,
     `allow-dev-connections`, the `direct_dev` entries in `DataSourcesService` (METHODS, validate,
     requireDevAllowed) and in the edit page (DevLogin, labels.ts). `ClientDatabaseConnections` keeps its pools and
     checks for the firewall-allow-list method, with the login from the secrets store instead.
   - **Client-database connections (next, to validate the external path):** a "Firewall allow-list" data source
     pointing at the owner's local Postgres (localhost / playpen, as `prism_reader`) must be able to Test connection
     and, once its facts and dimensions are registered, serve charts. Needs: (1) a secrets store behind an interface
     (local: a git-ignored file / environment variable per secret name; a real store such as AWS Secrets Manager
     later); (2) a provider for client databases: a pool per data source, the same login checks as SQL/022 (not
     Prism's login, read-only, no access to Prism's schema), time limit, audit; TLS required except an explicit
     localhost exception; Test connection and chart queries both use it. Firewall allow-list and private link are
     both a direct connection from Prism's side; outbound agent stays later.
   - after saving facts or dimensions of a `prism_database` source, call `prism.grant_star_tables_to_reader()`
     (SQL/022) so the read-only metric login can read exactly the registered tables;
   - **Facts and dimensions:** pick tables from the data source (with #6 schema reading), set aliases, and the
     fact–dimension links (join columns, suggested from foreign keys);
   - **Focus areas and levels:** an area per dimension; levels as dimension columns with a **label** and a **key**
     column (one label per key, checked), display order, and drill paths (several per area, as in the demo's two
     Time paths), with the same-area / no-loop rules;
   - **Metrics:** as below; with a **live preview** in the metric editor (owner, 2026-11-01: wanted): the metric as
     a chart on real data (read-only login) by a chosen focus level, refreshed as the definition changes, with
     sanity warnings (e.g. a percentage over 100 %, all values empty, a column that looks like a stored ratio);
   - the metadata setup assistant (#11) produces proposals these screens import and validate.
   Metric editing:
   - pick a fact, a type and columns;
   - row condition;
   - parse-based validation (JSqlParser allow-list incl. `GREATEST`/`LEAST`), with "did you mean" column
     suggestions;
   - **Test** (dry run with time and row limits, friendly error translation);
   - **Preview** with sanity warnings;
   - **View SQL**;
   - two reorderable lists per focus area: display order, and drill order with an "excluded" box.
6. **Schema reading** for client data sources (list tables and columns, suggest joins from foreign keys).
7. **Calculation templates / Custom type** (free-form expression through the same parser).
8. **Prism Advisor and community library** (MCP; vision in OVERVIEW §8, design in [MCP.md](MCP.md)). Being built
   on `feature-MCP-Server` ahead of the fact/dimension work (owner, 2026-10-22); skeleton verified 2026-10-22; guides, catalog drafts and `search_metric_catalog` done (owner review: §1); `review_star_schema` built 2026-10-24
   on `feature-MCP-Server-P2` (rules S0–S8, focus-level suggestions; verified in the Inspector); the three prompts
   (`choose-metrics`, `design-star-schema`, `review-schema`) built 2026-10-24 on the same branch, templates in
   `src/main/resources/advisor/prompts/` (owner review, like the guides). Later they also feed the metadata setup
   assistant (#11).
   Prompts verified on the owner's Mac (build, tests, Inspector, each workflow end to end), 2026-10-24: **phase 1
   complete and merged.** Phase 2 (`submit_insight`) needs a central service, so the fact/dimension branch (#1)
   comes first.
   - `review_star_schema` lists every `level_N` column and says to keep only the levels in use: organizations
     differ (e.g. team → manager → director → SVP) and the DDL can't show which levels hold data (owner, 2026-10-24).
   - Advisor: owner-curated metric catalog, guides, `review_star_schema` and `draft_metric` tools, guided prompts;
   - capture (`submit_insight`) and a curation queue; then give-to-get community access and enterprise-private
     knowledge once authentication exists (#4);
   - **owner to-do:** the metric catalog content (Claude can draft: DORA, flow, predictability, quality) and the
     open questions in OVERVIEW §8.2 (contract wording, leaving, paid option, seeding).
9. **"What drives this?" analysis** (OVERVIEW §8.4): within-team, lagged and conditional relationships between
   metrics; needs working-practice dimensions and an incidents fact.
10. **Percentile metric type** (OVERVIEW §8.5): `PERCENTILE_CONT` over raw rows; needs per-item grain.
11. **Metadata setup assistant** (raised by the owner, 2026-10-24): users won't know Prism's internal data model,
   so Prism helps them map their STAR schema onto `Data_Sources`, `Facts`, `Dimensions`, `Facts_Dimensions_Assoc`,
   `Focus_Areas` and `Focus_Levels` (drill pointers included), then onto `Metrics`. Placed here only so earlier
   numbers stay stable. It needs step 1 (the tables), and the in-app part fits with steps 5–6.
   - **One shared "configuration proposal" service:** input is DDL (`StarSchemaReviewService` / JSqlParser, not
     regex) or the live catalog of a connected data source (step 6, the better source: it's what is actually deployed).
     Output is a proposed set of metadata rows, with the questions it couldn't answer (e.g. which `level_N` columns
     hold data, display labels, drill order).
   - **Advisor (MCP):** the `design-star-schema` / `review-schema` prompts ask about the design and produce the
     proposal as a file (re-runnable SQL or JSON), like `draft_metric`'s INSERT. **The Advisor never writes metadata**
     until MCP authentication exists (MCP.md phase 3: enterprise and role from the token); even then it only
     submits a *draft* for an admin to approve.
   - **In-app wizard:** a signed-in metrics admin imports the proposal (or starts from the connected data source),
     answers the open questions, sees the validation results and a preview, and confirms. The enterprise comes from
     the app session, so the app is the security gate.
12. Later: import a pasted full query into a metric; client-API data sources (if revenue justifies);
   PrivateLink connections; per-cloud deployments (OVERVIEW §3.1); dashboard tabs and sharing (dashboards
   themselves: §1); save a dashboard's last focus level and filters; roles; enterprise-scoped metadata; super-user enterprise picker; integrations
   (e.g. Jira).

### Before production deployment (security; owner, 2026-11-01)

Prism runs only locally for now, so functional work comes first (next: #5). These must be done before any deployment
outside the owner's machine:

4. **Authentication / sessions** (backlog #5): server-side sessions (HttpOnly cookie), every `/api` endpoint behind
   a session (except login), 401 → back to Login; roles (OVERVIEW's `User_Roles`: viewer, dashboard author, metrics
   admin, enterprise admin) with role checks on the metadata admin screens (#5); and:
   - the server takes the enterprise and the user from the session (not `enterprisePk` / `enterprise_pk` /
     `X-User-ID` from the browser);
   - users with a temporary password are blocked from everything except Change Password;
   - users can't deactivate themselves;
   - CSRF protection, session timeout, CORS tightened (backlog #6); the MCP endpoint behind the same sessions.

## 3. Smaller backlog items (see CLAUDE.md "Open issues")

- #6 CORS (`file://`, `null`): revisit before deployment; add test/prod origins when known.
- #8 "works identically" check vs the old Groovy code on a live DB (deferred by owner).
- `Enterprises.temp_password_hash` is unused: drop it in a later script.
- Demo data: % Estimated is always 100%; Estimate Changed is non-zero for only 13 of 78 teams; completed points
  exceed committed in 86% of rows. **Owner decision: no effort on better demo data**; learn from real data.

## 4. Decided (for reference — details in OVERVIEW.md)

- No client STAR data in the Prism DB (demo schema only). The main path is a direct read-only connection;
  client APIs are a second phase.
- No Metric Groups: the fact table plays that role.
- Fact grain = one row per combination of the lowest-level dimensions; no duplicates; gaps mean N/A, never zero
  (OVERVIEW §4.1).
- Community library (OVERVIEW §8.2): give to get. Contributors get the collective knowledge, others are walled off
  from it (the Prism-authored base layer stays available to all). What's shared is knowledge about using data
  (calculation options, dimension designs, pitfalls, improvement paths, cross-client findings as direction and
  strength), never fact data.
- Improvement paths, quality order (owner): test quality → test coverage → integration and batch size (few open
  branches, short branch life, then small batches and frequent releases). It's never just one thing.
- Percentiles are never stored and averaged: a Percentile metric type computes them over raw rows (planned).
- Metric types: Percentage, Ratio, Average (ignores missing values), Sum. No Count (use Sum of 1 with a
  condition). All metric-specific information lives only in `Metrics`.
- Scope Churn = (added + removed) ÷ GREATEST(committed, completed), per row. Velocity = average of
  `avg_velocity_3_sprints`. Estimate Changed and % Estimated ÷ `total_stories`. Commitment Strength dropped
  (needs the previous sprint's average).
- Metadata cache: whole reload every 15 minutes; unknown ID → one reload, then 400.
- Calculation accuracy: any calculation-affecting change must pass an old-vs-new parity check (CLAUDE.md
  conventions).
- Temporary passwords: random per user, shown once with Copy, expire after the enterprise's hours (default
  72). Never a shared default.
- Users are deactivated, never deleted. A blank username is stored as NULL.
- Hosting: two or three cloud providers at most; each enterprise lives in one deployment; stay cloud-neutral.
- Connections to client databases: firewall allow-list (default), private link (enterprise), outbound agent
  (only if demanded).

## 5. Session notes (how Claude sessions work on this repo)

- Files reach the Mac only through the file bridge (see CLAUDE.md "Working notes"). The bridge can't delete
  files, and can't read files 7+ folders below `REST`.
- Claude can't reach Maven Central or npm. Checks run in the cloud workspace:
  - Java: `javac -Xlint:all -Werror` against hand-written API stubs;
  - frontend: `tsc` against type stubs, plus Playwright tests on an esbuild bundle with a mocked API;
  - SQL: a local Postgres 16 loaded with SQL 001–012 and the demo data.

  These stubs and harnesses live only in the session and must be recreated in a new session (cheap). The
  parity harness is kept in the repo: `REST/tools/parity`.
- **JSqlParser** (for testing `review_star_schema` against the real parser) can't be downloaded either, but GitHub
  is reachable: build JavaCC 7.0.13 from source with its bootstrap jar, then JSqlParser 5.3 from its tag (the
  grammar is a JavaCC `.jjt` file), and compile against the resulting jar. About 10 minutes.
