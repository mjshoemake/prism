# Designing Prism-compatible fact and dimension tables

Prism reads a **STAR schema**: fact tables that hold what happened, joined to dimension tables that describe it.
Prism writes every SELECT, JOIN, GROUP BY and WHERE itself, so the tables only need a clear shape. Table and column
names can be anything.

## 1. Decide the grain first

The **grain** is set by the fact table's dimensions: **one row per combination of the lowest level of each
dimension.** The demo's fact table has two dimensions whose lowest levels are sprint and team, so it has exactly one
row per sprint and team. A fact table with five dimensions has one row per five-way combination.

| Lowest levels (grain) | Example fact table | Good for |
|---|---|---|
| **team × sprint** | `Sprint_Metrics_Fact` (the demo) | Predictability: Say/Do, scope churn, velocity |
| **team × day × item type** (end-of-day snapshot) | work in progress, open branches | "How many were open?" measured over time |
| **deployment** (or branch, incident), with team and date keys | one row per deployment | Rates (change failure rate), durations, percentiles |

- **No duplicates, ever.** Enforce it with a unique key on the dimension keys (the primary key). Two rows for the
  same team and sprint double-count everything above them.
- **Gaps are fine, and they mean "N/A", never zero.** A team that first appears in sprint 6 has no rows for
  sprints 1–5. Don't fill them with zeros: Prism treats a missing combination as not applicable (no bar, and
  averages skip it), which is right. A zero row would claim the team existed and delivered nothing.
- **A real zero is a row.** If the team existed in that sprint and completed nothing, store the row with zeros.
- One grain per fact table. Don't mix sprint summaries and single deployments in one table.
- Pick the finest grain you can afford. Prism can always add rows up to team, manager or year; it can never split a
  summary back apart. **Percentiles (median, p90) need one row per item**, i.e. the item (branch, deployment) is the
  lowest-level dimension, even if it's only an ID column on the fact row.

## 2. Measures: store counts, not results

- Store **numerators and denominators** (committed points, completed points, failed deployments, deployments),
  never a pre-calculated percentage, ratio or average. Prism adds the counts up at each level and then divides.
  A stored percentage can't be added up correctly (see the 38 % example in `prism://guide/metric-types`).
- Measures should be numbers that can be added (counts, points, hours). Use `NOT NULL DEFAULT 0` when "none"
  really means zero, and leave NULL only when the value is genuinely unknown (Prism's Average ignores NULLs).
- Event-grain facts often need no measure column at all: each row counts as 1, and flags (`caused_failure`,
  `is_production`) become row conditions.

## 3. Dimensions

Every fact row points to its dimensions by key (`team_pk`, `sprint_pk`, `date_pk`, `service_pk`).

- **Time.** A sprint or date dimension with separate columns for each way people slice time: year, quarter,
  month, sprint name, plus combined labels that sort correctly (`2024 Q1`, `2024 01`). The combined labels let
  a drill-down from Year to Month keep years apart.
- **Organization.** A team dimension with one column per level of the hierarchy (`level_1_name` …
  `level_5_name`, `team_name`). Each column becomes a focus level; their order becomes the drill-down.
- **What the work is about** (when relevant): service or repository, product, environment, severity.
- **How the team works.** Working practices (share of automated testing, branching approach, review practice,
  release process) belong in a dimension too, so results can be split by them. A practice that changes over time
  needs history: either a row per team per period, or `valid_from` / `valid_to` columns.

**Share dimensions between fact tables.** If the deployment fact and the sprint fact both use the same team and
time dimensions, Prism can put them side by side and slice them the same way. Separate copies of "team" with
different spellings can't be combined.

### Statuses and snapshots

Flow metrics (WIP, burndown, cumulative flow, time in status) need to know what state each item was in, and when.

- **A status workflow that shows the real flow.** A recommended baseline for software work: *New* (being defined;
  never inside a sprint) → *Defined* (ready to be pulled in) → *In Progress* → *Dev Complete* (only stabilization and
  fixes left) → *Test Complete* → *Done*. "New → In Progress → Done" is the least overhead but hides when work reached
  the testers or the product owner.
- **Consider wait states between work states** (waiting for test, waiting for review, blocked) so the time work sits
  idle can be measured.
- **Daily snapshot facts.** One row per team per day (per item type and status) with counts and points at end of
  day. That's the grain for WIP per assignee, burndown, cumulative flow and "done by mid-sprint". Features usually
  need a weekly snapshot over a quarter.
- **Status changes as events.** One row per item per status change (from, to, timestamp) gives cycle time and time
  in status.
- **Derive sprint-level columns during loading** when a metric needs a moment in time, e.g. points done at the
  sprint's midpoint or completed on the last day, so the sprint fact keeps one row per team per sprint.

## 4. Labels and NULLs

- A dimension column's values are the labels on the chart. Keep them human-readable and unique at their level
  (two different teams must not both be called "Platform").
- An empty (NULL) hierarchy value shows as an empty label and can be filtered on. Decide whether that is intended
  (a team with no level-3 manager) or a data gap to fix.

## 5. Checklist

- [ ] The grain of each fact table is the combination of its dimensions' lowest levels, with a unique key on them.
- [ ] Missing combinations are left out (N/A), never filled with zeros.
- [ ] No stored percentages, ratios or averages; the raw counts are there instead.
- [ ] Every dimension key in a fact table has a dimension table behind it.
- [ ] A time dimension with year, quarter, month and sprint columns, plus combined labels.
- [ ] Hierarchy columns in the order people drill (company → … → team).
- [ ] Shared dimensions across fact tables.
- [ ] One row per item where you'll want medians or p90s.
- [ ] Working practices captured, with history, if you want to know which practices drive results.
- [ ] A status workflow that shows when work reached test and review, and daily snapshots for flow metrics.

Use the `review_star_schema` tool (coming) to check CREATE TABLE statements against these rules.
