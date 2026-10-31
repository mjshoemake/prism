-- 020-dashboards.sql
--
-- Saved dashboards (owner, 2026-10-31; docs/OVERVIEW.md §2, ROADMAP): a user's page of 1, 2 or 4 charts.
--
--   Dashboards        one per saved dashboard: its owner, a name (unique per owner) and its layout:
--                       1 = one chart (today's page), 2 = two charts one above the other, 4 = quadrants.
--   Dashboard_Charts  the metric shown in each chart area, by position (1–4, reading order: 1 top-left, 2 top-right,
--                     3 bottom-left, 4 bottom-right; in the 2-chart layout 1 is the top, 2 the bottom).
--                     Positions beyond the current layout are kept and only hidden, so switching back from 2 to 4
--                     charts brings them back (owner, 2026-10-31). An area without a row has no metric chosen yet.
--
-- The focus level and the drill-down filters are not saved: they are the user's current view, shared by every chart
-- on the dashboard (a click filters and drills all charts together). No tabs for now (owner, 2026-10-31).
-- Dashboards are deleted by their owner (unlike users, which are only deactivated); their charts go with them.
-- TODO (roadmap #4): the owner comes from the request's X-User-ID header until real sessions exist.
--
-- RE-RUNNABLE: CREATE ... IF NOT EXISTS, guarded constraints and trigger.

CREATE TABLE IF NOT EXISTS "prism"."Dashboards" (
  dashboard_pk  INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id       UUID        NOT NULL,
  name          VARCHAR(60) NOT NULL,
  layout        SMALLINT    NOT NULL DEFAULT 1,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "prism"."Dashboard_Charts" (
  dashboard_pk  INTEGER  NOT NULL,
  position      SMALLINT NOT NULL,
  metric_pk     INTEGER  NOT NULL,
  CONSTRAINT "Dashboard_Charts_pkey" PRIMARY KEY (dashboard_pk, position)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboards_user_fk') THEN
    ALTER TABLE "prism"."Dashboards" ADD CONSTRAINT dashboards_user_fk
      FOREIGN KEY (user_id) REFERENCES "prism"."Users" (user_id) ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboards_name_uk') THEN
    ALTER TABLE "prism"."Dashboards" ADD CONSTRAINT dashboards_name_uk UNIQUE (user_id, name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboards_layout_ck') THEN
    ALTER TABLE "prism"."Dashboards" ADD CONSTRAINT dashboards_layout_ck CHECK (layout IN (1, 2, 4));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboards_name_ck') THEN
    ALTER TABLE "prism"."Dashboards" ADD CONSTRAINT dashboards_name_ck CHECK (btrim(name) <> '' AND name = btrim(name));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_charts_dashboard_fk') THEN
    ALTER TABLE "prism"."Dashboard_Charts" ADD CONSTRAINT dashboard_charts_dashboard_fk
      FOREIGN KEY (dashboard_pk) REFERENCES "prism"."Dashboards" (dashboard_pk) ON UPDATE RESTRICT ON DELETE CASCADE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_charts_metric_fk') THEN
    ALTER TABLE "prism"."Dashboard_Charts" ADD CONSTRAINT dashboard_charts_metric_fk
      FOREIGN KEY (metric_pk) REFERENCES "prism"."Metrics" (metric_pk) ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_charts_position_ck') THEN
    ALTER TABLE "prism"."Dashboard_Charts" ADD CONSTRAINT dashboard_charts_position_ck CHECK (position BETWEEN 1 AND 4);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dashboards_set_updated_at'
                  AND tgrelid = '"prism"."Dashboards"'::regclass) THEN
    CREATE TRIGGER dashboards_set_updated_at BEFORE UPDATE ON "prism"."Dashboards"
      FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at();
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS dashboard_charts_metric_idx ON "prism"."Dashboard_Charts" (metric_pk);

COMMENT ON TABLE "prism"."Dashboards" IS 'A user''s saved dashboard: name and layout (1, 2 or 4 charts), SQL/020';
COMMENT ON COLUMN "prism"."Dashboards".layout IS '1 = one chart, 2 = two stacked, 4 = quadrants';
COMMENT ON TABLE "prism"."Dashboard_Charts" IS
  'Metric per chart area (position 1-4, reading order); positions beyond the layout are kept but hidden';

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('020-dashboards.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
