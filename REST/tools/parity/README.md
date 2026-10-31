# Metric parity check

Proves that a change to how metrics are calculated gives **identical results**. Required before any change that
could affect metric values ships (CLAUDE.md, "Calculations must stay exactly right").

`MetricParityTests` (`src/test/java/com/prism/api/metrics/`) sends chart requests through the real
`SprintMetricsService`: the same metadata cache, query builder and JDBC binding (`Types.OTHER`) the API uses, with
metric and focus-level definitions read from the database (nothing hand-copied). Results are compared as text
(labels, order, values with their decimals). It runs three checks:

| Check | Database | Compared with |
|---|---|---|
| **Hand-checked dataset**: 18 edge cases with answers worked out by hand (below) | `prism_fixture`, rebuilt each run by `fixture/fixture.sh` | `fixture/expected.tsv` |
| **Demo data**: every metric × focus level × filter combination (~23,500 cases, ~118,000 rows) | `playpen` | `baseline/demo-baseline.txt` (frozen) |
| **Check the check**: a plausible wrong calculation must be reported as different, for every metric | both | the two above |

## Run

```bash
cd ~/Documents/Work/prism/REST
PGPASSWORD=… bash tools/parity/run.sh            # compare (about 30 s plus the Maven build)
PGPASSWORD=… bash tools/parity/run.sh --freeze   # write a new baseline
```

`run.sh` rebuilds the fixture database with `psql` (taken from the PATH, else from Postgres.app or pgAdmin;
or set `PRISM_PSQL_BASE="/path/to/psql -h localhost -p 5432 -U postgres"`), then runs
`mvn test -Dtest=MetricParityTests -Dprism.parity=true`. Normal builds skip the test (it needs live databases).
Connection: `spring.datasource.*` from `application.properties`. The fixture database is the same URL with
`prism_fixture` as the database name. Override with `-Dprism.parity.url=…`, `-Dprism.parity.fixture-url=…`,
`-Dprism.parity.username=…`, `-Dprism.parity.password=…`; `fixture.sh` takes `PRISM_PSQL_BASE` and `FIXTURE_DB`.

On failure the report lists the first differing cases (expected vs actual). Full results are written to
`target/parity/demo-actual.txt` and `target/parity/fixture-actual.txt`, which can be diffed against the baseline.

## When to freeze a baseline

The baseline is the accepted answer for the demo data, so it is frozen **only on the owner's machine** (the
order of text labels depends on the database's collation) and only when results are known to be right:

1. **First time:** freeze from the current code, which was proven identical to the original queries
   (SQL/012: 24,160 cases, 0 differences).
2. **A change that must not change results** (e.g. the fact/dimension model): run without `--freeze`. It must
   report 0 differences. Do not freeze to make a failure go away.
3. **A change that is meant to change results** (e.g. a rounding rule): run, check that only the intended cases
   differ, agree the new values, then freeze, and commit the new baseline with the change.

The baseline also changes when the demo data or the metric/focus-level rows change; freeze again after
confirming why. Commit `baseline/demo-baseline.txt` (about 3 MB). `target/parity/` is not committed.

## What the demo-data cases cover

Generated from the database every run (so new metrics and levels are included automatically), per active metric:

- each focus level with no filter;
- each focus level filtered by every single value of every focus level, NULL included (`IS NULL`);
- drill-down paths: each value, then the next level in the area by `sort_order`, 3 levels deep for the first
  metric and 2 for the others. These are plain filter combinations, so they stay valid when
  `drill_to_focus_level_pk` replaces `sort_order` as the drill order;
- 300 seeded random combinations of 2–3 filters (same seed every run, so the same cases);
- a value with an apostrophe (`O'Brien`).

Filter values are taken from the keys the metrics return (step 1e: filters match a level's key column; for a level
without one the key is its label), so the generator doesn't depend on the FROM clause. Results are still compared
as `label|value`; a wrong key shows up in the filtered cases.
Cases are identified by names (`Say/Do Ratio | Year | Team=Team 7`), not IDs. Focus-level names must therefore be
unique, and renaming a metric or level means freezing again.

## Hand-checked dataset

`fixture/fixture-data.sql` adds 4 sprints, 5 teams and 7 fact rows. Every row is there for an edge case:

| Row | Sprint | Team | Committed / completed (Say/Do) | Other values that matter |
|---|---|---|---|---|
| f1 | 2025-01 | Team A | 10 / 9 | velocity 20.00; added 2 + removed 1, GREATEST(10, 11) = 11; modified 1, estimated 4 of 5 |
| f2 | 2025-01 | Team B | 40 / 10 | velocity 30.00; no churn, GREATEST = 40; 0 modified, 10 of 10 estimated |
| f3 | 2025-02 | Team A | 0 / 0 (zero denominator) | velocity NULL; everything else 0 |
| f4 | 2025-02 | O'Brien Squad | 20 / 15 | velocity 15.50; churn 5 + 0, GREATEST(20, 18) = 20; 2 modified, 8 of 8 |
| f5 | 2025-04 | Team D (NULL levels 1–3) | 20 / 10 | velocity 15.50 (tie); churn 0 + 5, GREATEST(20, 25) = 25; 3 modified, 6 of 6 |
| f6 | 2026-01 | Team A | 10 / 10 | velocity 22.25; no churn, GREATEST = 10; 0 modified, 5 of 5 |
| f7 | 2026-01 | O'Brien Squad | 10 / 5 | velocity NULL; churn 1 + 1, GREATEST(10, 5) = 10; 0 modified, 3 of 4 |

Team A and Team B share levels 2–5 (Mgr Lee …); O'Brien Squad reports to Mgr O'Brien; Team D has no level 1–3
names; Team E has no fact rows. Expected answers (`fixture/expected.tsv`), worked out by hand:

| Case | Calculation | Expected |
|---|---|---|
| Say/Do, Level 2, Sprint = 1 (key; label Sprint 25.01) | (9 + 10) / (10 + 40) | Mgr Lee **38.00** (not 57.50, the average of 90 % and 25 %; OVERVIEW §4.1) |
| Say/Do by Team | O'Brien (15+5)/(20+10); A (9+0+10)/(10+0+10); B 10/40; D 10/20 | 66.67, 95.00, 25.00, 50.00 |
| Say/Do by Level 2 | Lee (19+10)/(20+40); O'Brien 20/30; NULL 10/20 | 48.33, 66.67, NULL label 50.00 |
| Say/Do by Sprint, Team A | 9/10; 0/0; 10/10 | 90.00, **NULL** (no bar, no error), 100.00 |
| Say/Do by Month (all years) | Jan: (9+10+10+5)/(10+40+10+10); Feb 15/20; Apr 10/20 | Jan 48.57, Feb 75.00, Apr 50.00 (labels Jan/Feb/Apr, month order) |
| Say/Do by Year & Month | 19/50; 15/20; 10/20; 15/20 | Jan 2025 38.00, Feb 2025 75.00, Apr 2025 50.00, Jan 2026 75.00 (tie) |
| Say/Do by Year | 2025: 44/90; 2026: 15/20 | 48.89, 75.00 |
| Velocity by Team (AVG ignores NULL) | O'Brien 15.50; A (20 + 22.25)/2 = 21.125; B 30; D 15.50 | 15.50, **21.13** (rounds half up), 30.00, 15.50 (tie) |
| Velocity, Team A in Sprint = 2 (Sprint 25.02) | only a NULL value | Team A with NULL value |
| Scope Churn by Team | O'Brien (5+2)/(20+10); A 3/(11+0+10); B 0/40; D 5/25 | 23.33, 14.29, 0.00, 20.00 |
| Estimate Changed by Team | O'Brien 2/12; A 1/10; B 0/10; D 3/6 | 16.67, 10.00, 0.00, 50.00 |
| % Estimated by Team | O'Brien 11/12; A 9/10; B 10/10; D 6/6 | 91.67, 90.00, 100.00, 100.00 (tie) |
| Say/Do by Team, Team = Team E | no fact rows | empty result |
| Say/Do by Team, Level 2 IS NULL | Team D only | Team D 50.00 |
| Say/Do by Team, Level 2 = Mgr O'Brien | apostrophe bound as a value | O'Brien Squad 66.67 |
| Say/Do by Quarter (all years) | Q1: (9+10+0+15+10+5)/(10+40+0+20+10+10) = 49/90; Q2 10/20 | Q1 54.44, Q2 50.00 |
| Say/Do by Year & Sprint, Team A | key = start date; label "<sprint> <year>" (SQL/019) | Sprint 25.01 2025 90.00, Sprint 25.02 2025 NULL, Sprint 26.01 2026 100.00 |
| Say/Do by Year & Month, Month = 1 (a key) | Jan 2025 19/50; Jan 2026 15/20 | Jan 2025 38.00, Jan 2026 75.00 |

To add a case: add rows to `fixture-data.sql` if needed, work out the answer by hand, add it to `expected.tsv`
(format in the file's header), and add it to this table.

## File formats

`expected.tsv` and the baseline use the same format: `## <header>` starts a case, followed by one `label|value`
line per chart row in result order. `\N` is a NULL label, an empty value is NULL, and lines starting with `#` are
comments. A baseline header is the case key `metric | level | filters`. An expected header is
`description | metric | level | filters`. Filters are `-` or `Level=value` joined by ` & `, with `\N` for IS NULL.
Backslash escapes `\ | & = #` and line breaks inside names and values.

## History

The first parity check (SQL/012, Oct 2026) was a one-off: hand-written "old" queries against the new builder,
through `psql` (24,160 cases, 120,580 rows, 0 differences). This permanent check replaced it; the one-off scripts
were removed once the first baseline was frozen.

## Limitations

- The fixture uses the demo schema's tables (`Sprint_Metrics_Fact`, `Sprint_Dim`, `Team_Dim`). The fact/dimension
  model must keep working on it, or the fixture moves with it.
- Text ordering depends on collation, so a baseline frozen on one machine may not match another database.
  Fixture labels were chosen to sort the same in `C` and `en_US`.
- Exact text comparison: a deliberate change in decimals shows up as a difference everywhere (as intended).
