---
id: deployment-frequency
name: Deployment Frequency
summary: How many production deployments a team makes per period.
level: conditional
problems: [slow delivery, releasing slowly, infrequent releases, big releases, batch size, release cadence, time to market]
tags: [dora, throughput, deployments, batch-size]
type: sum
numerator: "1"
condition: "df.environment = 'production'"
unit: deployments
decimals: 0
grain: deployment (one row per deployment, with team and date keys)
related: [change-failure-rate, branches-merged-within-a-day]
prerequisites: [solid automated testing, fast CI]
---

## What it tells you

How often a team ships to production. More frequent deployments usually mean smaller changes, which are easier to
test and to roll back. That's the DORA finding, **if** the testing foundation is in place.

## How Prism calculates it

Sum of 1 per production deployment (a count), from a deployment fact (alias `df`). Group by month or sprint to read
it as "deployments per month".

## Pitfalls

- **Totals add up across teams.** At manager or company level this is the total for everyone, not a per-team
  figure. Compare teams at team level, over periods of the same length.
- **Conditional practice.** Small batches and frequent deployments are death to a team that relies on manual
  testing: it can't keep up, and big changes go out under-tested. Push frequency only after test quality and
  coverage (`prism://guide/improvement-paths`).
- **Gaming.** Splitting one change into many trivial deployments raises the number without changing anything.
  Read it with change failure rate and branch life.
- **Per-team rate.** "Deployments per team per week" averaged across teams is a different metric (it weights
  every team equally). Name it accordingly if you build it.

## Fits the improvement path

**Conditional**: step 4, after test quality, coverage and continuous integration.
