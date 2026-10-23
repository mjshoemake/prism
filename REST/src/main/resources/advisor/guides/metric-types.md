# Prism metric types

Every Prism metric has a **type** that tells Prism how to calculate it. You name the columns; Prism writes the
SQL. Every type recalculates from the raw fact rows at every drill-down level (team, manager, director;
sprint, month, quarter, year), so a number is always right for the level you are looking at.

## The four types

| Type | You provide | Prism calculates | Default display | Example |
|---|---|---|---|---|
| **Percentage** | numerator, denominator | `SUM(numerator) / SUM(denominator) × 100` | `%`, y-axis 0–100 | Say/Do ratio, change failure rate, % of branches merged within a day |
| **Ratio** | numerator, denominator | `SUM(numerator) / SUM(denominator)` | no unit, fit to data | story points per story, incidents per deployment |
| **Average** | value | `AVG(value)`: average per fact row; rows without a value are ignored, not counted as zero | fit to data | velocity per team-sprint |
| **Sum** | value | `SUM(value)` | fit to data | total story points; sprints with zero churn (value `1`, condition below) |

- A **zero denominator** gives an empty value (no bar), not an error.
- There is **no Count type**: a count is a Sum of `1` with a row condition (e.g. value `1`, condition
  `smf.story_points_added_during_sprint + smf.story_points_removed_during_sprint = 0` counts sprints with no churn).
- Values are rounded to the metric's **decimal places** (0–6, default 2).

## Row conditions

An optional **row condition** ("only rows where…") applies to *every* part of the calculation, so the numerator
and denominator always cover the same rows. Example: only completed sprints, only production deployments.
Prism adds it as `FILTER (WHERE condition)` on each aggregate.

## The most important rule: ratio of sums, never an average of percentages

Store the **counts** (numerator and denominator columns) in the fact table, never a pre-calculated percentage.
Prism adds up the counts at each level, then divides.

| Team | Completed | Committed | Team % |
|---|---|---|---|
| A | 9 | 10 | 90 % |
| B | 10 | 40 | 25 % |
| **Both** | **19** | **50** | **38 %** correct (ratio of sums) — not 57.5 % (average of 90 % and 25 %) |

Averaging stored percentages weights a team with 10 points the same as a team with 40, so the result is wrong at
every level above the stored one. This applies to every percentage and ratio metric.

**If your fact table only has a stored percentage** (e.g. `pass_rate`), go back to the source and add the two
counts it was calculated from. An Average of a stored percentage is the same "average of averages" error.

## Choosing a type

- "What share of X…?" → **Percentage** (numerator = the X rows, denominator = all rows).
- "How many A per B?" → **Ratio**.
- "What is a typical value per sprint/team row?" → **Average** — only if the value is a genuine per-row
  measure (like velocity), not a ratio.
- "How many / how much in total?" → **Sum** (of a column, or of `1` with a condition for a count).
- "Reduce failed deployments": usually means the **rate** (Percentage: failed ÷ all deployments), not the count.
  Chasing the count makes teams deploy less, which makes each change bigger and riskier.

## Percentiles (median, p90)

Not supported yet. A planned **Percentile** type will compute them over raw rows
(`PERCENTILE_CONT(p) WITHIN GROUP (ORDER BY value)`), which needs **one fact row per item** (e.g. per branch).
Never store a team's percentile and average it: Team A merged 20 branches in 1 day each, Team B 1 branch in 30 days
— the average of the two medians is 15.5 days, but the real median is 1 day. Until then, a Percentage with a row
condition ("% of branches merged within 1 day") answers most such questions and adds up correctly.

## Writing the expressions

- Use the fact table's alias (e.g. `smf.committed_story_points`); simple column expressions such as
  `smf.a + smf.b` or `GREATEST(smf.a, smf.b)` are fine.
- No `;`, no comments, no sub-queries.
- Metric names are unique (≤ 40 characters); titles ≤ 60; units ≤ 20.
