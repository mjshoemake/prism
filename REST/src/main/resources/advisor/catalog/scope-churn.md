---
id: scope-churn
name: Scope Churn
summary: Story points added or removed during the sprint, relative to the size of the sprint.
level: foundation
problems: [changing priorities, interruptions, unplanned work, drop everything, sprint scope keeps changing, lack of focus]
tags: [scrum, predictability, planning, focus]
type: percentage
numerator: "smf.story_points_added_during_sprint + smf.story_points_removed_during_sprint"
denominator: "GREATEST(smf.committed_story_points, smf.story_points_completed)"
unit: "%"
grain: team × sprint
related: [points-added-mid-sprint, net-scope-change, say-do-ratio, commitment-strength]
prerequisites: [added and removed points tracked during the sprint]
---

## What it tells you

How much the sprint's scope, which should be stable, churned and shifted underneath the team: interruptions,
reprioritization, "drop what you're doing" requests. Without focus there is almost certainly no predictability.
When something is added to a sprint, something planned won't get finished. You can decide what that is up front
(remove something of similar size that hasn't started), or be surprised when the sprint ends.

## How Prism calculates it

Percentage, from a sprint fact (alias `smf`): (points added + points removed) ÷ a measure of sprint size, as a
ratio of sums across rows. The higher the percentage, the bigger the hit to the team's focus and productivity.

## Variants (there are several ways to skin the cat)

| Denominator | Formula | Note |
|---|---|---|
| Committed points | (added + removed) ÷ committed | Simplest; churn relative to the plan |
| Committed + added | (added + removed) ÷ (committed + added) | Stays below 100 % more often when a lot is added |
| Larger of committed and completed | (added + removed) ÷ GREATEST(committed, completed) | Used by Prism's example; a sprint that grew a lot doesn't explode just because little was committed |

Pick one, write it down, and use it for every team. All three are Percentage metrics: change the denominator.

## Look at the raw numbers too

Scope churn hides direction. Chart added and removed scope separately (points or issue counts):

- **Primary goal:** shrink added scope (`points-added-mid-sprint`). Added scope causes inefficiency on its own.
- **Secondary goal:** keep added and removed close together (`net-scope-change`), so that whenever scope is added,
  something is removed to compensate. Adding more than you remove asks for work not to be completed.

## Pitfalls

- **Count both directions.** Removing work is churn too.
- **Re-estimates aren't churn.** Track changed estimates separately (the demo's Estimate Changed metric).
- **It's a conversation starter, not a stick.** It lets the team highlight decisions that hurt their delivery.
  Accountability goes both ways: most churn comes from outside the team.
- **High churn isn't always bad**, for example a team whose job is incident response. Compare like with like.

## Fits the improvement path

**Foundation** for predictability: protect the plan before asking for reliable delivery
(`prism://guide/improvement-paths`). It often explains a low Say/Do ratio.
