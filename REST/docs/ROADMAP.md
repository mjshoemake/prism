# Prism — Roadmap & Next Steps

> The single to-do list. Start each work session here. **Design decisions** live in [OVERVIEW.md](OVERVIEW.md)
> (§3.1 hosting, §4.2–4.3 data model, metric types, axis scaling, drill-down); **code facts, conventions
> and the numbered backlog** live in [CLAUDE.md](../CLAUDE.md). This file says *what to do next, in what order*.
> Update it whenever an item is finished or a new one is agreed.

_Last updated: 2026-10-24._

---

## 1. In review

Nothing at the moment. Article URLs are added to the Advisor content once the owner's *Agile Metrics* series is
further along (owner, 2026-10-23).

Done: Advisor phase 1 (MCP): content (guides, 15-entry catalog, prompt templates) reviewed by the owner, prompts
verified in the Inspector and each workflow run end to end in an assistant, branches merged (2026-10-24); repository cleanup for public viewing, root README, fresh-database setup script (2026-10-23); parity baseline frozen and committed, retired scripts deleted (2026-10-22: 23,557 cases, 118,168 rows,
0 differences); obsolete files deleted, migrations 010–012 run, Year / Quarter / Month filtering confirmed through
the real JDBC driver (2026-10-21).

## 2. Next build steps (agreed order)

1. **Feature branch `feature-metamodel`: fact/dimension model** (created by the owner after the MCP merge,
   2026-10-24; prove-or-fail-fast, merge when confident). Steps, each passing the parity check before the next:
   (a) ✅ SQL/013 tables + demo seed; (b) SQL/014 focus levels → dimension columns, drill pointers, sort column
   (#11); (c) query builder builds FROM/JOIN from the metadata, `DEMO_FROM_CLAUSE` deleted; (d) data-provider seam,
   drill pointers in the UI.
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
   - Switch on keys (after 1c, backlog #11): group/order by the key, return the key with each chart row, and filter
     by the key. Deliberate result change (sprint order, years no longer merged where the key separates them), so
     agree the differences and freeze a new baseline. Month-name labels (Jan, Feb) need label columns in the demo
     dimension, e.g. `month_name`.
   - `Metrics` point to a fact (`Facts`); the hard-coded FROM clause (`MetricQueryBuilder.DEMO_FROM_CLAUSE`) goes
     away. Prism adds only the joins a query needs.
   - **Drill-down pointer** `Focus_Levels.drill_to_focus_level_pk` (✅ metadata, SQL/015). A level without one doesn't
     drill: clicking adds the filter and stays. Checks: same focus area (FK), no loops (trigger). Seed: two Time paths
     (owner, 2026-10-24), Year → Year & Quarter → Year & Month → Year & Sprint, and Quarter → Month → Sprint;
     Organization: Level 5 → … → Team. Still to do: the API returns the pointer and the UI drills by it (step d).
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
5. **Metric admin screens:**
   - pick a type and columns;
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
   PrivateLink connections; per-cloud deployments (OVERVIEW §3.1); dashboards (tabs ≤ 4 charts, sharing,
   dashboard-wide drill-down); roles; enterprise-scoped metadata; super-user enterprise picker; integrations
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
