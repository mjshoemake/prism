---
id: branches-merged-within-a-day
name: Branches Merged Within a Day
summary: Share of merged feature branches that lived 24 hours or less.
level: conditional
problems: [long-lived branches, merge conflicts, continuous integration, working in isolation, batch size]
tags: [integration, branching, trunk-based, flow]
type: percentage
numerator: "CASE WHEN bf.branch_hours <= 24 THEN 1 ELSE 0 END"
denominator: "1"
condition: "bf.merged_at IS NOT NULL AND bf.branch_kind = 'feature'"
unit: "%"
grain: branch (one row per branch, with team and date keys)
related: [open-branches, deployment-frequency, change-failure-rate]
prerequisites: [fast and trustworthy automated tests, repositories mapped to teams]
---

## What it tells you

Whether a team really integrates continuously. Long-lived branches mean developers work in isolation, even when
the team follows a "trunk-based" scheme on paper. DORA describes trunk-based development as branches merged within
about a day, with few open at once.

## How Prism calculates it

Percentage, from a branch fact (alias `bf`, one row per branch): merged feature branches that lived ≤ 24 hours ÷
all merged feature branches × 100. It adds up correctly at every level, unlike an average or median of branch age.

## Data you need

- One row per branch: repository (mapped to a team), created or first-commit time, merge time, kind
  (feature / release / environment), and the computed `branch_hours`.

## Pitfalls

- **When does a branch start?** At creation or at its first commit. Pick one and keep it.
- **Exclude release and environment branches**, or they swamp the numbers (the condition above).
- **Abandoned branches** (never merged) aren't counted here. Watch them separately: they're a smell of their own.
- **Why not "average branch age"?** A few forgotten branches open for months distort an average. A median or p90
  needs Prism's planned Percentile type, and must never be stored per team and averaged (`prism://guide/metric-types`).

## Fits the improvement path

**Conditional**: step 3 (continuous integration). Safe once tests are fast and trustworthy.
