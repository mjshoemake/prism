---
id: points-added-mid-sprint
name: Points Added Mid-Sprint
summary: Story points added to the sprint after it started.
level: foundation
problems: [interruptions, unplanned work, changing priorities, drop everything, sprint scope keeps growing]
tags: [scrum, predictability, focus, scope]
type: sum
numerator: "smf.story_points_added_during_sprint"
unit: points
decimals: 0
grain: team × sprint
related: [scope-churn, net-scope-change]
prerequisites: [added points tracked during the sprint]
---

## What it tells you

The raw amount of interruption: work pushed into a sprint after planning. The primary goal of managing scope churn
is to shrink this number, because added scope creates significant inefficiency on its own.

## How Prism calculates it

Sum of points added during the sprint, from a sprint fact (alias `smf`). Chart it by sprint next to points removed
and the difference (`net-scope-change`). Use an issue count instead (value `smf.issues_added_during_sprint`) if
points aren't reliable.

## Pitfalls

- **Totals add up across teams.** At manager level it's the total for all teams. Compare teams at team level, and
  read it with sprint size (`scope-churn`).
- **Ask where it came from.** Most added scope comes from outside the team. The number is a discussion starter
  about those decisions, not a team score.

## Fits the improvement path

**Foundation** for predictability, together with scope churn.
