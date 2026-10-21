# Prism — Roadmap & Next Steps

> The single to-do list. Start each work session here. **Design decisions** live in [OVERVIEW.md](OVERVIEW.md)
> (§3.1 hosting, §4.2–4.3 data model, metric types, axis scaling, drill-down); **code facts, conventions
> and the numbered backlog** live in [CLAUDE.md](../CLAUDE.md). This file says *what to do next, in what order*.
> Update it whenever an item is finished or a new one is agreed.

_Last updated: 2026-10-21._

---

## 1. Waiting on the owner

Verified 2026-10-21: obsolete files deleted (`TemporaryPasswordFields.tsx`, `SprintMetricsRequest.java`,
`metricDefinitions.ts`), migrations 010–012 run, and filtering on Year / Quarter / Month works through the real
JDBC driver (`Types.OTHER` binding confirmed).

Still open (owner):
- **Your own account:** if it still has a temporary password, change it within 72 hours of running 010.
- **Optional:** run the parity check on your machine (`REST/tools/parity/README.md`).

## 2. Next build steps (agreed order)

1. **Feature branch: fact/dimension model** (owner creates the branch; prove-or-fail-fast, merge when confident).
   - New tables: Data Source (type `database`; `api` later), Fact Table, Dimension Table, Fact–Dimension link.
     Seed them with the demo schema.
   - `Focus_Levels` point to a table and column instead of free-text `column_name`.
   - `Metrics` point to a fact table; the hard-coded FROM clause (`MetricQueryBuilder.DEMO_FROM_CLAUSE`) goes
     away. Prism adds only the joins a query needs.
   - **Drill-down pointer** `Focus_Levels.drill_to_focus_level_pk`. A level without one is standalone:
     clicking adds the filter and stays. Checks: same focus area, no loops. Seed: Year → Year & Quarter →
     Year & Month → Sprint, with Quarter and Month standalone. Organization: Level 5 → … → Team.
   - Server reaches data through one "metric data provider" seam (database now, client API later).
   - **Parity first:** extend `REST/tools/parity` into a permanent check (JUnit against a test profile, or a
     script), plus a small **hand-checked dataset**. It should cover: zero denominators, NULLs, empty groups,
     apostrophes, ties, and the Team A/B 38%-vs-57.5% example. Freeze results as the baseline before switching.
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
8. Later: import a pasted full query into a metric; client-API data sources (if revenue justifies);
   PrivateLink connections; per-cloud deployments (OVERVIEW §3.1); dashboards (tabs ≤ 4 charts, sharing,
   dashboard-wide drill-down); roles; enterprise-scoped metadata; super-user enterprise picker; integrations
   (e.g. Jira).

## 3. Smaller backlog items (see CLAUDE.md "Open issues")

- #6 CORS (`file://`, `null`): revisit before deployment; add test/prod origins when known.
- #7 database credentials committed in `application.properties`.
- #8 "works identically" check vs the old Groovy code on a live DB (deferred by owner).
- #9 `test.bat` points to a stale jar name.
- `Enterprises.temp_password_hash` is unused: drop it in a later script.
- Demo data: % Estimated is always 100%; Estimate Changed is non-zero for only 13 of 78 teams; completed points
  exceed committed in 86% of rows. **Owner decision: no effort on better demo data**; learn from real data.

## 4. Decided (for reference — details in OVERVIEW.md)

- No client STAR data in the Prism DB (demo schema only). The main path is a direct read-only connection;
  client APIs are a second phase.
- No Metric Groups: the fact table plays that role.
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
