-- 021-dashboard_chart_y_axis.sql
--
-- Y-axis scale per dashboard chart (roadmap #2; owner, 2026-10-31). Set on the dashboard's Edit page, saved with the
-- chart area; there is no unsaved toggle on the chart itself.
--
--   y_axis_mode  metric    the metric's own default (Metrics.scale_mode / scale_min / scale_max); the default, so
--                          charts saved before this script look the same as before
--                zero_100  0 to 100; extends to a round number above the highest value when a value exceeds 100
--                fit       0 to a round number above the highest value
--                custom    y_axis_min and/or y_axis_max (at least one; min < max when both). These bounds are hard:
--                          a bar beyond the maximum is drawn to the top edge with a striped (barber-pole) end and its
--                          real value in the tooltip; a minimum above 0 is flagged on the chart. Scaling is visual
--                          only and never changes a calculated value (docs/OVERVIEW.md §4.3).
--   y_axis_min / y_axis_max  only for custom (NULL otherwise).
--
-- RE-RUNNABLE: ADD COLUMN IF NOT EXISTS, guarded constraints.

ALTER TABLE "prism"."Dashboard_Charts" ADD COLUMN IF NOT EXISTS y_axis_mode VARCHAR(10)   NOT NULL DEFAULT 'metric';
ALTER TABLE "prism"."Dashboard_Charts" ADD COLUMN IF NOT EXISTS y_axis_min  NUMERIC(14,4);
ALTER TABLE "prism"."Dashboard_Charts" ADD COLUMN IF NOT EXISTS y_axis_max  NUMERIC(14,4);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_charts_y_axis_mode_ck') THEN
    ALTER TABLE "prism"."Dashboard_Charts" ADD CONSTRAINT dashboard_charts_y_axis_mode_ck
      CHECK (y_axis_mode IN ('metric', 'zero_100', 'fit', 'custom'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_charts_y_axis_bounds_ck') THEN
    ALTER TABLE "prism"."Dashboard_Charts" ADD CONSTRAINT dashboard_charts_y_axis_bounds_ck CHECK (
      CASE WHEN y_axis_mode = 'custom'
           THEN (y_axis_min IS NOT NULL OR y_axis_max IS NOT NULL)
                AND (y_axis_min IS NULL OR y_axis_max IS NULL OR y_axis_min < y_axis_max)
           ELSE y_axis_min IS NULL AND y_axis_max IS NULL
      END);
  END IF;
END$$;

COMMENT ON COLUMN "prism"."Dashboard_Charts".y_axis_mode IS
  'metric (the metric''s default) | zero_100 | fit | custom (hard bounds y_axis_min and/or y_axis_max), SQL/021';
COMMENT ON COLUMN "prism"."Dashboard_Charts".y_axis_min IS 'Custom axis minimum; NULL = 0 (or none for other modes)';
COMMENT ON COLUMN "prism"."Dashboard_Charts".y_axis_max IS 'Custom axis maximum; NULL = fit to the data';

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('021-dashboard_chart_y_axis.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
