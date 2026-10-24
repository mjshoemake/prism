---
id: last-day-completion
name: Last-Day Completion
summary: Share of the sprint's completed story points that were completed on the last day.
level: foundation
problems: [everything finishes at the end, magic last day, testing squeezed, hidden progress, untrustworthy done]
tags: [flow, burndown, cumulative-flow, done, quality]
type: percentage
numerator: "smf.points_completed_last_day"
denominator: "smf.story_points_completed"
unit: "%"
decimals: 0
grain: team × sprint
related: [scope-done-by-mid-sprint, wip-per-assignee]
prerequisites: [daily status snapshots]
---

## What it tells you

A red flag for progress that "magically" appears at the end. A team that completed 9 points over the first 9 days
and 46 on the last day had 84 % last-day completion. If 90 % of stories reach the testers on the next-to-last day and
all are Done by the end, how thorough could the testing have been? Can the progress be trusted?

## How Prism calculates it

Percentage, from a sprint fact (alias `smf`): points completed on the last day ÷ all points completed × 100, as a
ratio of sums. The loading process takes `points_completed_last_day` from the daily snapshots.

## Pitfalls

- **One sprint is noise; a recurring pattern is the signal.**
- **Look at the cumulative flow** to see why: an "In Progress" band that grows all sprint, or sub-tasks created on
  the fly instead of planned (flat task lines), are the usual causes.

## Fits the improvement path

**Foundation**: work flowing to Done through the sprint, before trusting quality or velocity.
