---
id: commitment-strength
name: Commitment Strength
summary: Story points committed at sprint start as a percentage of the team's recent average velocity.
level: foundation
problems: [overcommitment, overcommitting, commit to more than we finish, unrealistic plans, set up to fail, realistic expectations, unfinished sprints]
tags: [scrum, predictability, planning, velocity]
type: percentage
numerator: "smf.committed_story_points"
denominator: "smf.velocity_baseline_points"
unit: "%"
decimals: 0
grain: team × sprint
related: [velocity, say-do-ratio, scope-churn]
prerequisites: [relative sizing with story points, a velocity baseline recorded at sprint start]
---

## What it tells you

Whether the team is setting itself up for failure. Commitment should roughly match average velocity (about 100 %).
A team that regularly commits well above it under-performs: people spread their time across too many items, spend
hours on work they could never have finished, and put work they could have finished at risk. Regularly falling
short also creates a sense of inevitability ("we're always set up to fail anyway"), and the team stops owning the
goal. Teams perform better when they're confident rather than discouraged.

## How Prism calculates it

Percentage, from a sprint fact (alias `smf`): committed points ÷ the velocity baseline × 100, as a ratio of sums.
`velocity_baseline_points` is the team's average completed points over the **previous** 3–4 sprints, recorded when
the sprint starts. It must not include the sprint being measured.

## Data you need

- Committed points at sprint start (the same figure Say/Do uses).
- The velocity baseline, calculated by the loading process from earlier sprints and stored on the sprint row.

## Pitfalls

- **Watch the trend in new teams.** As average velocity rises, commitment often rises faster. The team keeps
  committing beyond what it has shown it can deliver.
- **A planned dip is fine.** Commitment and velocity both dropping (time off, a process change) is the team seeing
  it coming, not a problem.
- **Not a target to push up.** The goal is realistic commitment, not bigger commitment.

## Fits the improvement path

**Foundation** for predictability: realistic expectations, before asking for a better Say/Do ratio.
