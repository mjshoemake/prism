---
id: say-do-ratio
name: Say/Do Ratio
summary: Share of the story points committed at sprint start that were completed.
level: outcome
problems: [missed commitments, missing commitments, keep missing sprint goals, honoring commitments, do what you say, predictability, sprint planning]
tags: [scrum, predictability, planning]
type: percentage
numerator: "smf.committed_story_points_completed"
denominator: "smf.committed_story_points"
unit: "%"
grain: team × sprint
related: [scope-churn, commitment-strength, velocity]
prerequisites: [commitment recorded at sprint start]
---

## What it tells you

How reliably a team does what it says it's going to do: honoring commitments. It's the demo metric in Prism and the example behind the
"ratio of sums" rule.

## How Prism calculates it

Percentage, from a sprint fact (alias `smf`): committed points completed ÷ committed points × 100, as a ratio of
sums. Two teams at 90 % (9 of 10) and 25 % (10 of 40) are **38 %** together, not 57.5 %.

## Pitfalls

- **Use only the committed work that was completed**, not all completed work. Work added mid-sprint belongs in
  scope churn.
- **It's easy to game by under-committing.** Read it with commitment strength and scope churn, never as a
  target alone.
- **Low Say/Do usually has a cause upstream:** over-commitment (`commitment-strength`), interruptions
  (`scope-churn`) or too much WIP (`wip-per-assignee`). Work on the cause, not the ratio.
- **Record the commitment at sprint start.** A commitment edited later makes the number meaningless.

## Fits the improvement path

An **outcome** of the predictability path: low WIP, protected scope and realistic commitments come first.
