-- 015-focus_levels_dimensions_drill_keys.sql
--
-- Second step of the fact/dimension model (docs/ROADMAP.md §2 step 1b). Adds the metadata only: the code doesn't read
-- any of the new columns yet, so every chart stays exactly as it is (parity: 0 changed values).
--
--   Focus_Areas.dimension_pk           the dimension a focus area slices by (Time -> Sprint_Dim, Organization ->
--                                      Team_Dim). Its focus levels are columns of that dimension.
--   Focus_Levels.column_name           (existing) the LABEL column: the text a bar shows, e.g. "Sprint 9". Doesn't
--                                      need to be unique.
--   Focus_Levels.key_column_name       the KEY column: what Prism will group, sort and filter by, unique per bar,
--                                      e.g. sprint_start_date. NULL = the label column is the key (today's behaviour).
--                                      Each key value must have one label (owner, 2026-10-24).
--   Focus_Levels.drill_to_focus_level_pk  the next level down when a bar is clicked; NULL = no drill (the click only
--                                      adds the filter). Same focus area (composite foreign key), no loops (trigger).
--                                      Replaces "next sort_order" as the drill order; sort_order is display order only.
--
-- Seeds two Time hierarchies (owner, 2026-10-24) and the Organization one:
--   Year -> Year & Quarter -> Year & Month -> Year & Sprint      (new level: label sprint_abbr, key sprint_start_date)
--   Quarter -> Month -> Sprint                                    (Sprint: label sprint_name, key sprint_num)
--   Level 5 -> Level 4 -> Level 3 -> Level 2 -> Level 1 -> Team
-- Display order: Year, Year & Quarter, Year & Month, Year & Sprint, Quarter, Month, Sprint.
--
-- Parity: the new level adds cases, and the drill path Year & Month -> next level changes, so the check reports
-- missing / unexpected cases but must report 0 differing values. Then freeze a new baseline (tools/parity/README.md).
--
-- RE-RUNNABLE: guarded DDL; existing keys and drill pointers are never overwritten.

-- ---- Focus_Areas.dimension_pk ------------------------------------------------------------------
ALTER TABLE "prism"."Focus_Areas" ADD COLUMN IF NOT EXISTS dimension_pk INTEGER;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_areas_dimension_fk') THEN
    ALTER TABLE "prism"."Focus_Areas" ADD CONSTRAINT focus_areas_dimension_fk
      FOREIGN KEY (dimension_pk) REFERENCES "prism"."Dimensions" (dimension_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
END$$;

UPDATE "prism"."Focus_Areas" fa
   SET dimension_pk = d.dimension_pk
  FROM (VALUES ('Time', 'Sprint_Dim'), ('Organization', 'Team_Dim')) AS v(area, table_name)
  JOIN "prism"."Dimensions" d    ON d.schema_name = 'prism' AND d.table_name = v.table_name
  JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = d.data_source_pk AND ds.name = 'Demo data'
 WHERE fa.name = v.area
   AND fa.dimension_pk IS NULL;

-- Required once every area has one (on a database with other, unlinked areas this waits for them).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "prism"."Focus_Areas" WHERE dimension_pk IS NULL) THEN
    ALTER TABLE "prism"."Focus_Areas" ALTER COLUMN dimension_pk SET NOT NULL;
  ELSE
    RAISE WARNING 'Focus_Areas without a dimension_pk: column left nullable';
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS focus_areas_dimension_idx ON "prism"."Focus_Areas" (dimension_pk);

-- ---- Focus_Levels: key column, drill pointer, rules --------------------------------------------
ALTER TABLE "prism"."Focus_Levels" ADD COLUMN IF NOT EXISTS key_column_name        VARCHAR(30);
ALTER TABLE "prism"."Focus_Levels" ADD COLUMN IF NOT EXISTS drill_to_focus_level_pk INTEGER;

DO $$
BEGIN
  -- Column names are written into SQL unquoted (MetricQueryBuilder), so they must be plain identifiers.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_columns_ck') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT focus_levels_columns_ck
      CHECK (column_name ~ '^[A-Za-z_][A-Za-z0-9_]*$'
             AND (key_column_name IS NULL OR key_column_name ~ '^[A-Za-z_][A-Za-z0-9_]*$'));
  END IF;

  -- Names identify levels in the UI and in the parity check's case keys.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_name_uk') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT focus_levels_name_uk UNIQUE (focus_area_pk, name);
  END IF;

  -- Target of the drill pointer's composite foreign key (level + its area).
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_area_uk') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT focus_levels_area_uk UNIQUE (focus_level_pk, focus_area_pk);
  END IF;

  -- The next level must be in the same focus area.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_drill_to_fk') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT focus_levels_drill_to_fk
      FOREIGN KEY (drill_to_focus_level_pk, focus_area_pk)
      REFERENCES "prism"."Focus_Levels" (focus_level_pk, focus_area_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'focus_levels_drill_to_self_ck') THEN
    ALTER TABLE "prism"."Focus_Levels" ADD CONSTRAINT focus_levels_drill_to_self_ck
      CHECK (drill_to_focus_level_pk IS DISTINCT FROM focus_level_pk);
  END IF;
END$$;

-- No loops (A -> B -> A): follow the pointers from the changed level; coming back to it is refused.
CREATE OR REPLACE FUNCTION "prism".focus_levels_no_drill_loop() RETURNS trigger AS $$
DECLARE
  found_loop BOOLEAN;
BEGIN
  IF NEW.drill_to_focus_level_pk IS NULL THEN
    RETURN NULL;
  END IF;
  WITH RECURSIVE path(level_pk, depth) AS (
      SELECT NEW.drill_to_focus_level_pk, 1
    UNION ALL
      SELECT fl.drill_to_focus_level_pk, p.depth + 1
        FROM path p
        JOIN "prism"."Focus_Levels" fl ON fl.focus_level_pk = p.level_pk
       WHERE fl.drill_to_focus_level_pk IS NOT NULL
         AND p.level_pk <> NEW.focus_level_pk
         AND p.depth < 1000
  )
  SELECT EXISTS (SELECT 1 FROM path WHERE level_pk = NEW.focus_level_pk) INTO found_loop;
  IF found_loop THEN
    RAISE EXCEPTION 'Focus level % (%): drilling down would loop back to it', NEW.focus_level_pk, NEW.name
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'focus_levels_no_drill_loop') THEN
    CREATE CONSTRAINT TRIGGER focus_levels_no_drill_loop
      AFTER INSERT OR UPDATE OF drill_to_focus_level_pk ON "prism"."Focus_Levels"
      DEFERRABLE INITIALLY DEFERRED
      FOR EACH ROW EXECUTE FUNCTION "prism".focus_levels_no_drill_loop();
  END IF;
END$$;

COMMENT ON COLUMN "prism"."Focus_Levels".column_name IS
  'Label column: the text a bar shows (need not be unique)';
COMMENT ON COLUMN "prism"."Focus_Levels".key_column_name IS
  'Key column: group, sort and filter by it, unique per bar, one label per key; NULL = the label column (SQL/015)';
COMMENT ON COLUMN "prism"."Focus_Levels".drill_to_focus_level_pk IS
  'Next level down on click (same focus area, no loops); NULL = no drill-down from this level';
COMMENT ON COLUMN "prism"."Focus_Levels".sort_order IS 'Display order in pickers (drill order: drill_to_focus_level_pk)';
COMMENT ON COLUMN "prism"."Focus_Areas".dimension_pk IS 'The dimension this area slices by; its levels are its columns';

-- ---- Seed: new level Year & Sprint -------------------------------------------------------------
INSERT INTO "prism"."Focus_Levels" (focus_area_pk, name, column_name, key_column_name, sort_order)
SELECT fa.focus_area_pk, 'Year & Sprint', 'sprint_abbr', 'sprint_start_date', 35
  FROM "prism"."Focus_Areas" fa
 WHERE fa.name = 'Time'
ON CONFLICT (focus_area_pk, name) DO NOTHING;

-- ---- Seed: key columns (only where still empty) --------------------------------------------------
UPDATE "prism"."Focus_Levels" fl
   SET key_column_name = v.key_column_name
  FROM (VALUES ('Time', 'Sprint', 'sprint_num')) AS v(area, level, key_column_name)
  JOIN "prism"."Focus_Areas" fa ON fa.name = v.area
 WHERE fl.focus_area_pk = fa.focus_area_pk
   AND fl.name = v.level
   AND fl.key_column_name IS NULL;

-- ---- Seed: drill pointers (only where still empty) -----------------------------------------------
UPDATE "prism"."Focus_Levels" fl
   SET drill_to_focus_level_pk = target.focus_level_pk
  FROM (VALUES
    ('Time', 'Year',           'Year & Quarter'),
    ('Time', 'Year & Quarter', 'Year & Month'),
    ('Time', 'Year & Month',   'Year & Sprint'),
    ('Time', 'Quarter',        'Month'),
    ('Time', 'Month',          'Sprint'),
    ('Organization', 'Level 5', 'Level 4'),
    ('Organization', 'Level 4', 'Level 3'),
    ('Organization', 'Level 3', 'Level 2'),
    ('Organization', 'Level 2', 'Level 1'),
    ('Organization', 'Level 1', 'Team')
  ) AS v(area, level, drill_to)
  JOIN "prism"."Focus_Areas" fa      ON fa.name = v.area
  JOIN "prism"."Focus_Levels" target ON target.focus_area_pk = fa.focus_area_pk AND target.name = v.drill_to
 WHERE fl.focus_area_pk = fa.focus_area_pk
   AND fl.name = v.level
   AND fl.drill_to_focus_level_pk IS NULL;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('015-focus_levels_dimensions_drill_keys.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
