---
id: mutation-score
name: Mutation Score
summary: Share of deliberately introduced small bugs (mutants) that the tests catch.
level: foundation
problems: [test quality, weak tests, false confidence, escaped defects, coverage gaming]
tags: [quality, testing, test-quality]
type: percentage
numerator: "mr.killed_mutants"
denominator: "mr.killed_mutants + mr.survived_mutants"
unit: "%"
grain: module × run (one row per module per mutation-testing run, with team and date keys)
related: [change-failure-rate]
prerequisites: [a mutation-testing tool in the build (e.g. PIT for Java), runs on a schedule or on changed code]
---

## What it tells you

Whether the tests check anything. Coverage says which code the tests ran; mutation score says whether they would
have noticed it breaking. It's the most direct of the test-quality stand-ins.

## How Prism calculates it

Percentage, from a mutation-run fact (alias `mr`, one row per module per run): killed ÷ (killed + survived) × 100,
as a ratio of sums. Modules with many mutants weigh more, as they should.

## Data you need

- Per module and run: killed and survived mutants (leave out mutants the tool marks as equivalent or timed out),
  and the team that owns the module.

## Pitfalls

- **Never average module scores.** A tiny module at 100 % would count as much as a large one at 40 %. Store
  the counts and let Prism add them up.
- **It's slow to run.** Run it nightly or only on changed code, and use the latest run per period (e.g. a
  condition on a `is_latest_in_period` flag).
- **Survivors aren't all equal.** Some survive in code that doesn't matter. Look at where they are before
  setting targets.

## Fits the improvement path

**Foundation**: step 1 (test quality). Improve it before pushing coverage, and well before pushing batch size.
