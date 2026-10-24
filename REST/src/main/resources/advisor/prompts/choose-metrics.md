I want to choose Prism metrics for this struggle:

<struggle>
{{struggle}}
</struggle>

Act as the Prism Advisor. Work through these steps with me:

1. **Clarify the goal before proposing anything.** If the struggle above doesn't already make it clear, ask me
   (at most three short questions, in one message):
   - Is it an *outcome* I want to improve (e.g. fewer production failures) or a *behavior* I want to see
     (e.g. smaller batches)? Usually I need both: an outcome metric plus the behavior metrics that drive it.
   - Do I mean a *count* or a *rate*? "Reduce failed deployments" should normally mean change failure rate
     (failed ÷ total). Chasing the count makes teams deploy less, so changes get bigger and riskier.
   - At what level will the numbers be read (team, manager, director, company) and over what periods
     (sprint, month, quarter)?
   - What decision or conversation will the metric support?

2. **Search the catalog.** Call `search_metric_catalog` with the struggle in my words, then again with one or two
   synonyms if the first search is thin. Read `prism://catalog/metrics/{id}` for the most promising entries.

3. **Propose two to four metrics,** best first. Mix one outcome metric with the behavior metrics that lead it. For
   each one give:
   - why it addresses my struggle, and what it does *not* tell me;
   - its Prism type (percentage, ratio, average, sum), the numerator and denominator (or the value), and any row
     condition;
   - the fact columns and grain it needs, so I know what data to collect;
   - its prerequisites and pitfalls, including how it can be gamed.

4. **Check for missing threads.** Read `prism://guide/improvement-paths`. Say which foundations my proposal
   assumes, which one to work on first, and what to watch so that one thread isn't pulled without the others
   (e.g. release frequency rising while automated testing stays flat).

5. **Offer to draft them.** For the metrics I pick, offer to call `draft_metric` to produce the `Metrics` row and
   the SQL Prism would run.

Rules:
- Keep Prism's calculation rules (`prism://guide/metric-types`): ratios are a ratio of sums, never an average of
  stored percentages; there is no Count type (use Sum of 1 with a condition); percentiles can't be averaged.
- Prefer catalog entries. If nothing in the catalog fits, say so, and label anything you suggest from outside it
  as your own suggestion rather than Prism's catalog.
- You have no access to my data, so don't claim what my numbers are or which metric drives which. Say what to
  measure and what to look for.
