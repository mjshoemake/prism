-- 009-focus_sort_order_and_keys.sql
--
-- Gives focus areas and focus levels an explicit display / drill-down order, and gives
-- Focus_Levels a primary key and a foreign key to Focus_Areas.
--
--   Focus_Areas.sort_order    order of focus areas in the UI
--   Focus_Levels.sort_order   drill-down order within a focus area (lower = higher level,
--                             e.g. Year before Month); the UI drills to the next sort_order
--   Focus_Levels.focus_level_pk  surrogate key (needed by future exclusion tables)
--
-- Existing rows keep today's order: areas by focus_area_pk, levels by their current physical
-- row order, numbered 10, 20, 30... so new levels can be slotted in between later.
--
-- RE-RUNNABLE: every step checks what already exists; rows that already have a sort_order
-- are never renumbered.

-- ---- Focus_Areas.sort_order -------------------------------------------------------------------
ALTER TABLE "prism"."Focus_Areas" ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE "prism"."Focus_Areas"
   SET sort_order = focus_area_pk * 10
 WHERE sort_order IS NULL;

ALTER TABLE "prism"."Focus_Areas" ALTER COLUMN sort_order SET NOT NULL;

-- ---- Focus_Levels.sort_order ------------------------------------------------------------------
-- Numbered before the key column is added, while the physical row order is still the original one.
ALTER TABLE "prism"."Focus_Levels" ADD COLUMN IF NOT EXISTS sort_order INTEGER;

WITH numbered AS (
    SELECT fl.ctid AS row_id,
           COALESCE(MAX(fl.sort_order) OVER (PARTITION BY fl.focus_area_pk), 0)  -- continue after existing values
             + 10 * ROW_NUMBER() OVER (PARTITION BY fl.focus_area_pk, fl.sort_order IS NULL ORDER BY fl.ctid)
             AS new_sort_order,
           fl.sort_order
      FROM "prism"."Focus_Levels" fl
)
UPDATE "prism"."Focus_Levels" fl
   SET sort_order = n.new_sort_order
  FROM numbered n
 WHERE fl.ctid = n.row_id
   AND n.sort_order IS NULL;

ALTER TABLE "prism"."Focus_Levels" ALTER COLUMN sort_order SET NOT NULL;

-- ---- Focus_Levels primary key -----------------------------------------------------------------
ALTER TABLE "prism"."Focus_Levels"
    ADD COLUMN IF NOT EXISTS focus_level_pk INTEGER GENERATED ALWAYS AS IDENTITY;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'Focus_Levels_pkey') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT "Focus_Levels_pkey" PRIMARY KEY (focus_level_pk);
  END IF;
END$$;

-- ---- Focus_Levels -> Focus_Areas foreign key --------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_focus_area_fk') THEN
    ALTER TABLE "prism"."Focus_Levels"
      ADD CONSTRAINT focus_levels_focus_area_fk
      FOREIGN KEY (focus_area_pk) REFERENCES "prism"."Focus_Areas" (focus_area_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
END$$;

-- Supports "levels of an area, in order".
CREATE INDEX IF NOT EXISTS focus_levels_area_sort_idx
    ON "prism"."Focus_Levels" (focus_area_pk, sort_order);
