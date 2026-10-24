---
id: wip-per-assignee
name: WIP per Assignee
summary: Items in progress at end of day per team member working in the sprint, averaged over the sprint.
level: foundation
problems: [too much work in progress, multitasking, task switching, everything finishes at the end, juggling, work spread too thin]
tags: [wip, flow, efficiency, focus]
type: ratio
numerator: "ws.items_in_progress"
denominator: "ws.sprint_assignees"
condition: "ws.item_type = 'sub-task'"
unit: per person
decimals: 2
grain: team × day × item type (end-of-day snapshot)
related: [feature-wip-per-team, scope-done-by-mid-sprint, last-day-completion]
prerequisites: [a daily end-of-day snapshot of item statuses, assignees recorded on items]
---

## What it tells you

Whether team members focus on one thing at a time. Stop starting and start finishing. A developer juggling six
sub-tasks finishes most of them near the last day, so everything hits the testers at once and work rolls into the
next sprint. That person is manufacturing risk. Five finished tasks and one not started beats six that are all
"90 % complete": the only percentage you can trust is 100 %.

**Target: below about 1.3.** Occasionally having two things in flight (one blocked) is normal; it should be the
exception, not the rule.

## How Prism calculates it

Ratio, from a daily snapshot fact (alias `ws`, one row per team per day per item type): items in progress at end of
day ÷ unique assignees in the sprint. Because the number of assignees is the same every day of the sprint, the
ratio of sums equals the average of the daily values. At manager level it's the true per-person figure across all
the teams.

## Variants

- **Sub-task WIP** (condition `ws.item_type = 'sub-task'`) for teams that assign sub-tasks to people.
- **User-story WIP** (condition `ws.item_type = 'story'`) for teams that assign stories instead. Same target.

## Data you need

- An end-of-day snapshot per team: items in progress, by item type.
- The number of unique assignees of that item type in the sprint, on each row.

## Pitfalls

- **Drill down to the person** to find out why a team's WIP is high, but as a conversation, not a scorecard.
- **WIP is managed at every layer:** individual, team (`feature-wip-per-team`) and organization.
- **Swarming lowers it further:** several people on one item finishes it sooner. So does doing the shortest tasks first.

## Fits the improvement path

**Foundation**: the first step to real predictability is fewer things in flight.
