# Focus areas, focus levels and drill-down

Prism charts one metric at a time, grouped by one **focus level**. Clicking a bar **drills down**: the clicked
value becomes a filter, and the chart moves to the next level.

## Focus areas and levels

- A **focus area** is a way to slice the data, defined on one dimension: *Time* (the sprint/date dimension),
  *Organization* (the team dimension), later *Service*, *Product* and so on.
- A **focus level** is one column of that dimension: Year, Year & Quarter, Year & Month, Sprint; Level 5 … Team.
- Every metric whose fact table is linked to the dimension can be sliced by all of its levels. A metric can opt
  out of specific areas or levels.

## Display order and drill order

- **Display order** is the order levels appear in the level picker.
- **Drill order** is where a click goes next. Today it follows display order. A planned change gives each level
  its own "next level" pointer, so the two can differ.
- A level with no next level is **standalone**: clicking a bar adds the filter and the chart stays on that
  level. Example: "Month (all years)" compares every January with every February, so drilling further makes no sense.

Typical setup:

| Area | Drill path | Standalone |
|---|---|---|
| Time | Year → Year & Quarter → Year & Month → Sprint | Quarter (all years), Month (all years) |
| Organization | Level 5 → Level 4 → … → Level 1 → Team | — |

## Filters stay when you switch area

Drilling from *Year 2024 → Q3* and then switching to *Organization* shows every team for Q3 2024. The selection
is "rotated" onto another dimension. That's intended: it's how you ask "who drove Q3?".

## Gotchas

- **Labels must be unique at their level, or bars merge.** "Sprint 1" exists in every year. Grouping all sprints
  by `sprint_name` without a year filter adds the Sprint 1s of different years into one bar. Use a label that
  includes the year (`2024 Sprint 1`), or reach Sprint only by drilling from a year.
- **Combined time labels must sort as text.** `2024 01` sorts correctly; `Jan 2024` does not.
- **Empty labels** (NULL) form their own bar, and clicking it filters on "is empty". If that bar is large, it's
  usually a data gap.
- **Ratios stay right at every level** because Prism recalculates from the fact rows. You never need separate
  "rolled-up" columns per level.
