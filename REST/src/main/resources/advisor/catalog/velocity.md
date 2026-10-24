---
id: velocity
name: Velocity
summary: Story points completed per sprint; for planning and forecasting, never for measuring or comparing teams.
level: outcome
problems: [sprint planning, forecasting, how long will it take, throughput, delivery dropping]
tags: [scrum, throughput, planning, forecasting, trailing-indicator]
type: average
numerator: "smf.story_points_completed"
unit: points
decimals: 1
grain: team × sprint
related: [commitment-strength, say-do-ratio]
prerequisites: [relative sizing with story points (not hours converted to points)]
---

## What it tells you

How much a team typically completes in a sprint. **Velocity should never be used to measure team performance or to
compare teams.** Team 1 at 85 and Team 2 at 20 tells you nothing about which is doing better: points are relative
to each team. There is no single metric that shows how well a team delivers; a collection of metrics together does.

Used as intended, velocity is valuable for:

- **Sprint planning:** the next sprint shouldn't hold more than is reasonable.
- **Long-term planning:** forecasting how long features will likely take.
- **Throughput monitoring:** spotting shifts. When it drops or rises, find out why (someone left, a process change
  such as test coverage added to the definition of done, better automation, or overwork that isn't sustainable).

## How Prism calculates it

Average of completed points per sprint row, from a sprint fact (alias `smf`), per team. Grouped by quarter, it's the
team's average velocity for that quarter. The best view shows, per sprint: points completed, points committed at
sprint start, and the average over the last 3–4 sprints (`commitment-strength` turns the last two into one number).

## Pitfalls

- **Comparing teams invites gaming.** Teams inflate estimates when leaders use velocity as a measuring stick, and
  that destroys its accuracy for planning. Blown accuracy means blown usefulness.
- **Above team level the number means little.** A manager-level "average velocity" mixes different point scales.
- **Don't ask for velocity to go up.** It's a trailing indicator. Ask about the behaviors behind it (WIP, scope
  churn, commitment strength) instead.
- **Accuracy depends on relative sizing.** Estimating in hours and converting to points never gets as accurate.
- **Don't store and average a rolling average.** Keep completed points per sprint and let Prism average them.

## Fits the improvement path

An **outcome** (trailing indicator), improved through the foundation behaviors, never pushed directly.
