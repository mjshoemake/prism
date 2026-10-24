---
id: net-scope-change
name: Net Scope Change
summary: Points added minus points removed during the sprint; above zero means the sprint grew.
level: foundation
problems: [sprint scope grows, set up to fail, unfinished work, adding without removing, nothing removed when work is added]
tags: [scrum, predictability, scope]
type: sum
numerator: "smf.story_points_added_during_sprint - smf.story_points_removed_during_sprint"
unit: points
decimals: 0
grain: team × sprint
related: [scope-churn, points-added-mid-sprint]
prerequisites: [added and removed points tracked during the sprint]
---

## What it tells you

Whether added scope is compensated. Anytime more is added than removed, the sprint's total scope grows, which asks
for planned work not to be completed and sets the team up to fail. The goal is to keep this close to zero: when
something comes in, something of similar size that hasn't started goes out.

## How Prism calculates it

Sum of (added − removed) points, from a sprint fact (alias `smf`). It's the "Diff" line next to the added and
removed bars.

## Pitfalls

- **Zero isn't "no churn".** Adding 20 and removing 20 is still a lot of churn. Read it with `scope-churn` and
  `points-added-mid-sprint`.
- **Totals add up across teams**, so a large positive team and a large negative team cancel out at manager level.
  Look at teams individually.

## Fits the improvement path

**Foundation** for predictability: the secondary goal of managing scope churn.
