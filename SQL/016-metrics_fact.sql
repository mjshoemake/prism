-- 016-metrics_fact.sql
--
-- Third step of the fact/dimension model (docs/ROADMAP.md §2 step 1c): every metric belongs to one fact table.
--
--   Metrics.fact_pk   the fact (SQL/013) the metric is calculated from. Its expressions use that fact's alias
--                     (e.g. smf.committed_story_points) and may use the aliases of the fact's linked dimensions.
--
-- From this step on the API builds each chart query's FROM / JOIN from the metadata: the metric's fact, plus only
-- the dimensions the query needs (the focus level, the filters, and dimension aliases in the metric's expressions).
-- The hard-coded demo FROM clause is gone. Parity: the check must report 0 differences.
--
-- Seeds: the five demo metrics (all on Sprint_Metrics_Fact) belong to the demo fact.
--
-- RE-RUNNABLE: guarded DDL; existing fact links are never overwritten.

ALTER TABLE "prism"."Metrics" ADD COLUMN IF NOT EXISTS fact_pk INTEGER;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'metrics_fact_fk') THEN
    ALTER TABLE "prism"."Metrics" ADD CONSTRAINT metrics_fact_fk
      FOREIGN KEY (fact_pk) REFERENCES "prism"."Facts" (fact_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
END$$;

-- Every metric so far uses the demo fact's alias (smf).
UPDATE "prism"."Metrics" m
   SET fact_pk = f.fact_pk
  FROM "prism"."Facts" f
  JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = f.data_source_pk AND ds.name = 'Demo data'
 WHERE f.schema_name = 'prism' AND f.table_name = 'Sprint_Metrics_Fact'
   AND m.fact_pk IS NULL;

-- Required once every metric has one.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "prism"."Metrics" WHERE fact_pk IS NULL) THEN
    ALTER TABLE "prism"."Metrics" ALTER COLUMN fact_pk SET NOT NULL;
  ELSE
    RAISE WARNING 'Metrics without a fact_pk: column left nullable (the API skips those metrics)';
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS metrics_fact_idx ON "prism"."Metrics" (fact_pk);

COMMENT ON COLUMN "prism"."Metrics".fact_pk IS
  'The fact the metric is calculated from; expressions use its alias and its linked dimensions'' aliases (SQL/016)';

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('016-metrics_fact.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
