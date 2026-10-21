# Metric parity check

Proves that a change to how metrics are calculated gives **identical results**: every metric × focus level ×
filter combination is run through an "old" path and the "new" path against the same database, and the results
are compared exactly (labels, order, values as text). First used for SQL/012 (Oct 2026): 24,160 cases,
120,580 rows, 0 differences; a deliberate rounding change was detected in 11,792 cases.

- **Old path** (`parity.py`): the pre-SQL/012 query template, with the old UI's WHERE clause (quotes doubled).
  Say/Do uses the old UI's expression verbatim; the other metrics use hand-written SQL, independent of the builder.
- **New path** (`Gen.java`): `MetricQueryBuilder` with the metric rows as seeded by SQL/012, written as
  `PREPARE … EXECUTE` with untyped parameters (Postgres infers types, like the JDBC `Types.OTHER` binding).
- **Cases:** no filter; every single filter value on every level; drill-down paths (each bar, next `sort_order`
  level, 3 deep for Say/Do, 2 for others); 300 seeded random 2–3-filter combinations per metric; an apostrophe value.

## Run (needs the demo data, SQL 001–012, and compiled classes)

```bash
cd REST && ./mvnw -q compile                     # target/classes
cd tools/parity
export PRISM_PSQL="psql -h localhost -p 5432 -U postgres -d playpen"   # PGPASSWORD=… if needed
python3 parity.py                                # writes levels.tsv, cases.tsv, old.sql
javac -cp ../../target/classes -d . Gen.java && java -cp ../../target/classes:. Gen   # writes new.sql
$PRISM_PSQL -At -q -F '|' -v ON_ERROR_STOP=1 -f old.sql > old.out
$PRISM_PSQL -At -q -F '|' -v ON_ERROR_STOP=1 -f new.sql > new.out
python3 cmp.py old.out new.out                   # "differing cases 0" = identical
```

**Check the check:** edit `new.sql` (e.g. `sed 's/\* 100, 2)/* 100, 1)/' new.sql > mut.sql`), run it the same way,
and confirm `cmp.py old.out mut.out` reports differences. Always run old, new and mutated output with the
**same psql flags**, or the comparison is meaningless.

When the old path is retired (fact/dimension work), freeze `new.out` as the baseline and compare future
changes against it. Generated files (`*.tsv`, `*.sql`, `*.out`) are not meant to be committed.

## Notes and limitations (read before reusing)

- **Metric definitions are duplicated** in two places, and both must match the `Metrics` table:
  - `Gen.java` (new path) holds the rows exactly as seeded by SQL/012;
  - `parity.py` (`OLD` dict) holds the hand-written "old" expressions.

  If a metric is added or changed, update both, or make `Gen.java` read the rows from the database.
  `parity.py` reads focus levels and their distinct values from the database, so those are always current.
- **Drill paths follow `sort_order`** (the next level within the area), as the UI did before drill pointers.
  When `drill_to_focus_level_pk` exists, change `next_level` in `parity.py` to follow it.
- **NULL labels aren't covered:** the demo data has no NULLs in any focus-level column.
  - The old UI showed a NULL as "Unknown" and filtered on the text `'Unknown'`, which matched nothing (a bug).
  - The new path filters with `IS NULL`.

  The permanent harness should add NULLs to its hand-checked dataset.
- **Zero denominators aren't covered by the demo data.** The old Say/Do expression had no `NULLIF` and
  would have failed with a division-by-zero error; the new one returns NULL (no bar). Expected and intended;
  cover it in the hand-checked dataset.
- **Parameter typing:** filter values are written as untyped `PREPARE`/`EXECUTE` parameters, which Postgres
  infers the same way as JDBC `Types.OTHER` (what the server uses). That makes the comparison faithful, but it
  doesn't exercise the real JDBC driver; the running app does.
- **Same data, same flags:** both paths must run against the same database state and with identical `psql`
  output flags. A mismatch in separators alone makes every non-empty case "differ" (this happened once).
- **Speed:** about 24k cases × 2 run in roughly 25 seconds against a local Postgres.

## What the permanent version needs (roadmap: fact/dimension branch)

1. Run automatically (a JUnit test under a test profile, or one script), with no hand-copied metric rows.
2. A small **hand-checked dataset** with known answers. It should include:
   - zero denominators, NULL values and NULL labels;
   - empty groups, apostrophes and ties;
   - the same team in two months;
   - the Team A/B example (38%, not 57.5%; OVERVIEW §4.1).
3. A **frozen baseline** (`new.out` from the last accepted version), so later changes are compared against it
   once the old path no longer exists.
4. The mutation check as part of the run: a deliberately broken variant must produce differences.
