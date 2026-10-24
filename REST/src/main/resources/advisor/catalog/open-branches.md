---
id: open-branches
name: Open Branches (daily average)
summary: Average number of feature branches open at the same time, per team per day.
level: conditional
problems: [too many branches, work in progress, merge conflicts, continuous integration]
tags: [integration, branching, wip, flow]
type: average
numerator: "bs.open_feature_branches"
unit: branches
decimals: 1
grain: team × day (end-of-day snapshot)
related: [branches-merged-within-a-day]
prerequisites: [a daily snapshot job, repositories mapped to teams]
---

## What it tells you

How much parallel, unintegrated work a team carries. DORA's trunk-based description is fewer than three active
branches. Many open branches usually mean long-lived branches and painful merges.

## How Prism calculates it

Average, from a daily snapshot fact (alias `bs`, one row per team per day): the average of the daily counts over
whatever period you view (sprint, month, quarter).

## Data you need

- A job that records, once a day, each team's number of open feature branches.

## Pitfalls

- **It's a snapshot, so never sum it.** Adding daily counts over a month gives "branch-days", not branches.
  Average is the right type.
- **Exclude long-lived release and environment branches.**
- **Team size matters.** Ten open branches is a lot for three developers and normal for thirty. Compare a team
  with itself over time, or build a per-developer ratio.

## Fits the improvement path

**Conditional**: step 3 (continuous integration), read together with branches merged within a day.
