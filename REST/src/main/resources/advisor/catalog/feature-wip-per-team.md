---
id: feature-wip-per-team
name: Feature WIP per Team
summary: Features or epics a team has in progress at the same time, averaged over the quarter.
level: foundation
problems: [too many features in flight, features take many sprints, spread too thin, late delivery of features]
tags: [wip, flow, features, epics, efficiency]
type: average
numerator: "fs.features_in_progress"
unit: features
decimals: 1
grain: team × week (end-of-week snapshot)
related: [wip-per-assignee]
prerequisites: [features or epics as parents of stories, a weekly snapshot of feature status]
---

## What it tells you

Whether a team drives each feature to done or spreads itself across many. Working 1–2 stories of each of 7+
features per sprint stretches every feature out over many sprints. Working on 2–3 at a time delivers some
features weeks or months earlier.

**Target: below about 3 to 4.**

## How Prism calculates it

Average, from a weekly snapshot fact (alias `fs`, one row per team per week): features in progress at end of week.
Grouped by quarter (or program increment), it's the team's average feature WIP for the quarter.

## Pitfalls

- **It's a snapshot, so never sum it.** Average is the right type.
- **Shared features count for every team working on them.** Decide whether that's what you want to see.

## Fits the improvement path

**Foundation**: WIP at the team and feature layer, alongside `wip-per-assignee`.
