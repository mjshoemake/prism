---
id: change-failure-rate
name: Change Failure Rate
summary: Share of production deployments that cause a failure (rollback, hotfix, incident).
level: outcome
problems: [failed deployments, production incidents, release quality, outages, rollbacks, hotfixes]
tags: [dora, quality, stability, deployments]
type: percentage
numerator: "CASE WHEN df.caused_failure THEN 1 ELSE 0 END"
denominator: "1"
condition: "df.environment = 'production'"
unit: "%"
grain: deployment (one row per deployment, with team and date keys)
related: [deployment-frequency, branches-merged-within-a-day, mutation-score]
prerequisites: [an agreed definition of "failure", deployments attributed to a team or service]
---

## What it tells you

How often a change to production goes wrong. It's one of the four DORA measures and the usual answer to "reduce
failed deployments": the **rate**, not the count.

## How Prism calculates it

Percentage, from a deployment fact (alias `df`, one row per deployment): failed deployments ÷ all production
deployments × 100, recalculated at every level (team, manager, month, year). With a team × period summary fact
instead, use the counts: numerator `f.failed_deployments`, denominator `f.deployments`.

## Data you need

- One row per production deployment, with team (or service) and date keys.
- A `caused_failure` flag, set when a deployment led to a rollback, hotfix or incident. It's often filled in
  later, when the incident is linked back.

## Pitfalls

- **Count vs rate.** Chasing the count of failures makes teams deploy less, so changes get bigger and riskier.
- **"Failure" must mean the same thing for every team** (rollback? any incident? only Sev1/Sev2?). Write it down.
- **Late attribution.** Failures found days later change last month's number; expect recent periods to move.
- **Not a target on its own.** A team can lower it by deploying rarely. Read it with deployment frequency.

## Fits the improvement path

An **outcome**: improved through test quality, coverage and continuous integration (`prism://guide/improvement-paths`).
