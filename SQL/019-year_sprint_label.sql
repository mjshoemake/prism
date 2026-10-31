-- 019-year_sprint_label.sql
--
-- Year & Sprint bars show the year at the end of the label (owner, 2026-10-31), e.g. "Sprint 26 2024", like
-- "Q4 2024" and "Dec 2024": viewed across several years, a label without the year can't be told apart.
-- The all-years Sprint level keeps "Sprint 26": each of its bars combines that sprint number from every year.
--
-- 1. Demo Sprint_Dim: generated label column year_sprint_label = sprint_name || ' ' || sprint_year.
-- 2. The Year & Sprint level shows it (only if still on SQL/018's label, so an edited level is left alone).
--    Its key stays sprint_start_date, so grouping, order and filters don't change: a label-only change.
--
-- RE-RUNNABLE: ADD COLUMN IF NOT EXISTS; the update only touches the level while it still has the old label.

ALTER TABLE "prism"."Sprint_Dim" ADD COLUMN IF NOT EXISTS year_sprint_label VARCHAR(60)
  GENERATED ALWAYS AS (sprint_name || ' ' || sprint_year::text) STORED;

COMMENT ON COLUMN "prism"."Sprint_Dim".year_sprint_label IS 'Display label, e.g. Sprint 26 2024 (generated, SQL/019)';

UPDATE "prism"."Focus_Levels" fl
   SET column_name = 'year_sprint_label'
  FROM "prism"."Dimensions" d
  JOIN "prism"."Focus_Areas" fa ON fa.dimension_pk = d.dimension_pk
 WHERE d.schema_name = 'prism' AND d.table_name = 'Sprint_Dim'
   AND fl.focus_area_pk = fa.focus_area_pk
   AND fl.name = 'Year & Sprint'
   AND fl.column_name = 'sprint_name';

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('019-year_sprint_label.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
