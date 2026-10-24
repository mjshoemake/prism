# Improvement paths: it's never just one thing

Software delivery is a tapestry of practices that work best when no threads are missing. A practice that helps one
team can hurt another that lacks its foundation. So Prism recommends **ordered improvement paths**, checked
against a team's own data, rather than single best practices.

## Using metrics with teams

- **Objective data, not opinions.** Without data, a coach's advice is just an opinion and easy to dismiss. Metrics
  work like a lane departure warning system: they show a team drifting toward the ditch before it hits the tree.
- **Every recommendation needs a payoff.** If the return can't be stated convincingly, it won't get traction.
- **Partner with teams; don't use metrics against them.** The goal isn't accountability in a twisted form, but
  learning together how to deliver more reliably and with better quality.
- **One metric at a time.** Don't put 25 metrics in front of a team and expect them all to stay green. Each metric
  represents a behavior. Pick one, drive it to green together, and introduce the next only when the behavior has
  become muscle memory.
- **Leading vs trailing indicators.** Some metrics track a specific behavior (leading: WIP, scope churn, commitment
  strength). Others give a high-level sense of how things are going (trailing: velocity, Say/Do, change failure
  rate). Never ask a team to push a trailing indicator ("get velocity up"); they'll inflate estimates. Ask about the
  behaviors behind it, one root cause at a time.

## Levels of benefit

| Level | Helps | Examples |
|---|---|---|
| **Foundation** | Almost any team, whatever else it does | Test quality, automated test coverage; low WIP, protected sprint scope, realistic commitments |
| **Conditional** | Only on top of the foundation; can backfire without it | Real continuous integration (few open branches, short-lived branches), then small batches and frequent releases |
| **Outcomes** | What leaders ask for; improved by the levels above, not directly | Change failure rate, Sev1/Sev2 production incidents, time to restore, Say/Do, velocity |

Example of a conditional practice backfiring: small batches and frequent deployments are death to a team that
lives and dies by manual testing. They can't keep up with the rate of change, and significant changes go out
without enough testing.

## The quality path, in order

1. **Improve test quality.** Tests that run code without checking it give false confidence. Coverage from weak
   tests is worth little.
2. **Improve automated test coverage**, especially of the code that changes.
3. **Integrate continuously.** Keep few branches open at once and merge them quickly (aim: most within a day).
   Long-lived branches mean developers work in isolation, even under a "trunk-based" branching scheme on paper.
4. **Shrink batch size and release more often.** Now each change is small, tested and easy to roll back.

## The predictability path, in order

1. **Limit work in progress.** One thing at a time per person (`wip-per-assignee`, target below about 1.3), a
   few features at a time per team (`feature-wip-per-team`, below about 3–4). Work should reach Done throughout
   the sprint (`scope-done-by-mid-sprint`, at least 25–30 %), not on the last day (`last-day-completion`).
2. **Protect the sprint scope.** Interruptions destroy focus (`scope-churn`); shrink added scope
   (`points-added-mid-sprint`) and compensate for what's added (`net-scope-change`).
3. **Commit realistically.** Commit about what average velocity says the team can deliver
   (`commitment-strength`).
4. **Then** Say/Do and velocity become reliable, and velocity becomes useful for forecasting. They are the
   outcomes, not the levers.

## Readiness: when to move to the next step

Use the team's own numbers, not self-assessment:

- Before pushing **integration and batch size**: automated coverage of changed code is steady or rising, and
  escaped defects (found in production) are falling.
- Before pushing **release frequency**: most branches merge within a day, and the change failure rate is stable.

**Warning pattern: one thread pulled without the others.** Example: release frequency doubled this quarter, but
automated testing hasn't moved. This pattern tends to come before more failed changes.

## Measuring test quality

Test quality is hard to measure objectively. These stand-ins, together, make it trackable:

- **Mutation score:** small bugs are introduced on purpose; the share the tests catch (catalog: `mutation-score`).
- **Escaped defects in covered code:** production defects in code that tests ran.
- **Flaky-test rate:** tests that fail and pass at random teach people to ignore failures.

## Watch out for

- **Targets get gamed.** A coverage target produces tests that run code without checking anything. Pair
  coverage with a test-quality measure.
- **Count vs rate.** "Reduce failed deployments" should mean the change failure rate. Chasing the count makes
  teams deploy less, so each change gets bigger and riskier.
- **Correlation isn't causation.** When a practice and an outcome move together, check whether the
  foundation is in place. The strongest evidence is a known change: "Team 7 moved to short-lived branches in
  March; here's before and after."
