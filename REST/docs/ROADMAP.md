# Prism — Roadmap & Next Steps

> The single to-do list. Start each work session here. **Design decisions** live in [OVERVIEW.md](OVERVIEW.md)
> (§3.1 hosting, §4.2–4.3 data model, metric types, axis scaling, drill-down); **code facts, conventions
> and the numbered backlog** live in [CLAUDE.md](../CLAUDE.md). This file says *what to do next, in what order*.
> Update it whenever an item is finished or a new one is agreed.

_Last updated: 2026-10-24._

---

## 1. In review

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
   - **Owner:** run SQL/019 + SQL/020 together (019 was waiting for the next database change; then parity, expect
     only Year & Sprint label differences, and `--freeze`), build, try `/#/dashboards`.

Article URLs are added to the Advisor content once the owner's *Agile Metrics* series is
further along (owner, 2026-10-23).

Done: Advisor phase 1 (MCP): content (guides, 15-entry catalog, prompt templates) reviewed by the owner, prompts
verified in the Inspector and each workflow run end to end in an assistant, branches merged (2026-10-24); repository cleanup for public viewing, root README, fresh-database setup script (2026-10-23); parity baseline frozen and committed, retired scripts deleted (2026-10-22: 23,557 cases, 118,168 rows,
0 differences); obsolete files deleted, migrations 010–012 run, Year / Quarter / Month filtering confirmed through
the real JDBC driver (2026-10-21).

## 2. Next build steps (agreed order)

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
2. **Y-axis Scale control** on the chart: 0–100 (percentages), Fit to data, Custom min/max.
   - Bars beyond the maximum get a **barber-pole** top, and the real value shows on hover.
   - A raised minimum is flagged on the chart.
   - Choice remembered per metric for the session (per chart once dashboards exist).
   - Today a metric's `fixed` scale is only a suggestion, until this exists.
3. **Read-only database login** for metric queries (SELECT on STAR tables only, statement timeout), as a
   second database connection.
4. **Authentication / sessions** (backlog #5):
   - the server takes the enterprise from the session;
   - users with a temporary password are blocked from everything except Change Password;
   - users can't deactivate themselves;
   - fold in **#3** (missing fields → 400 via `@ControllerAdvice`).
5. **Metadata admin screens** (owner, 2026-10-31: configuring Prism for a new database must be possible in the UI,
   without knowing Prism's internal tables; metrics admins only, enterprise from the session, so after #4):
   - **Data sources:** add / edit a data source (connection method, host, database, the secret's name; never the
     credentials themselves), **Test connection**;
   - **Facts and dimensions:** pick tables from the data source (with #6 schema reading), set aliases, and the
     fact–dimension links (join columns, suggested from foreign keys);
   - **Focus areas and levels:** an area per dimension; levels as dimension columns with a **label** and a **key**
     column (one label per key, checked), display order, and drill paths (several per area, as in the demo's two
     Time paths), with the same-area / no-loop rules;
   - **Metrics:** as below;
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
