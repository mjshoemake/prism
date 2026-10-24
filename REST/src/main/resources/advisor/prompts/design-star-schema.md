Help me design Prism-compatible fact and dimension tables for the data my team has:

<description>
{{description}}
</description>

Act as the Prism Advisor. First read `prism://guide/star-schema`, `prism://guide/metric-types` and
`prism://guide/drill-down`. Then work through these steps with me:

1. **Understand the data.** Ask me only what the description above doesn't answer (one message, short questions):
   - which metrics I want to see (if I don't know yet, suggest we use the `choose-metrics` prompt first);
   - what one record is: a sprint per team, a deployment, a branch, an incident;
   - the lowest level of each dimension: team (and the organization levels above it that hold data), time
     (sprint, day, month), service or repository;
   - whether I'll need medians or percentiles (they need one fact row per item, e.g. per branch).

2. **Propose the tables** as PostgreSQL DDL:
   - one fact table per grain; the grain is one row per combination of the lowest-level dimension keys, enforced
     by a unique key on exactly those keys (a surrogate key alone doesn't enforce it);
   - measures stored as additive numerator and denominator columns, never percentages, ratios or averages;
   - dimension tables with a primary key, and foreign keys from the fact table to each of them;
   - hierarchy columns (e.g. organization levels, year → quarter → month) as columns of the dimension, so they
     can become focus levels; only the levels I actually use;
   - a time dimension (or a date key) so every metric can be sliced by period;
   - NOT NULL on measures unless NULL has a deliberate meaning.

   Explain each table in a sentence: what one row is, and which of my metrics it serves.

3. **Check it.** Call `review_star_schema` with the complete DDL. Fix every error, explain each warning and either
   fix it or say why it's acceptable, and call the tool again until it reports no errors.

4. **Show the result:** the final DDL, the review's suggested focus areas and drill order, and for each metric I
   wanted, its Prism type, numerator and denominator (offer `draft_metric` for them).

Rules:
- Gaps in the data mean N/A, never zero. Never suggest filling missing combinations with zero rows; a real zero is
  a stored row of zeros.
- Use my names for things where they're clear; otherwise lower_snake_case.
- Don't ask me for real data values; the design only needs the structure.
