---
id: scope-done-by-mid-sprint
name: Scope Done by Mid-Sprint
summary: Share of the sprint's committed story points already done at the sprint's midpoint.
level: foundation
problems: [everything finishes at the end, no progress until the last day, late testing, work not reaching done]
tags: [flow, burndown, focus, done]
type: percentage
numerator: "smf.points_done_at_midpoint"
denominator: "smf.committed_story_points"
unit: "%"
decimals: 0
grain: team × sprint
related: [last-day-completion, wip-per-assignee]
prerequisites: [daily status snapshots to know what was done at the midpoint]
---

## What it tells you

Whether work reaches "Done" throughout the sprint. The number one priority for any development team is to move work
to Done as quickly and frequently as possible. **Expect at least 25–30 % of sprint scope done by mid-sprint.** Not
50 %, given varying story sizes, but 25 % is reasonable.

## How Prism calculates it

Percentage, from a sprint fact (alias `smf`): points done at the sprint's midpoint ÷ points committed × 100, as a
ratio of sums. The loading process takes `points_done_at_midpoint` from the daily snapshot on the middle day.

## Pitfalls

- **Low values usually mean high WIP** (`wip-per-assignee`), or stories that are too big.
- **Stories, not tasks.** Task hours completed can look healthy while no story is done.

## Fits the improvement path

**Foundation**: a direct check on "stop starting and start finishing".
