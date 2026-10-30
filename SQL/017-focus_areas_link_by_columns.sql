-- 017-focus_areas_link_by_columns.sql
--
-- Repairs SQL/015 on databases where a focus area has another name than the seed's ('Time', 'Organization'),
-- e.g. 'Team Hierarchy'. 015 matched areas by name, so such an area got no dimension_pk (the API then skips its
-- focus levels: "Unknown focus level") and no drill pointers.
--
-- 1. Each focus area still without a dimension_pk is linked to the demo dimension whose table has every one of the
--    area's level columns, found from the database catalog; only when exactly one dimension fits.
-- 2. Drill pointers for the area linked to Team_Dim, by level name (Level 5 -> ... -> Level 1 -> Team), and for
--    the area linked to Sprint_Dim (both Time paths), where still empty.
-- 3. Focus_Areas.dimension_pk becomes NOT NULL once every area has one.
--
-- RE-RUNNABLE: only fills what is still empty.

-- ---- 1. Link areas to the dimension that holds their columns -----------------------------------
WITH candidates AS (
    SELECT fa.focus_area_pk, d.dimension_pk
      FROM "prism"."Focus_Areas" fa
      JOIN "prism"."Dimensions" d    ON TRUE
      JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = d.data_source_pk
                                    AND ds.db_connection_method = 'prism_database'
     WHERE fa.dimension_pk IS NULL
       AND EXISTS (SELECT 1 FROM "prism"."Focus_Levels" fl WHERE fl.focus_area_pk = fa.focus_area_pk)
       AND NOT EXISTS (
             SELECT 1
               FROM "prism"."Focus_Levels" fl
              WHERE fl.focus_area_pk = fa.focus_area_pk
                AND NOT EXISTS (SELECT 1
                                  FROM information_schema.columns c
                                 WHERE c.table_schema = d.schema_name
                                   AND c.table_name   = d.table_name
                                   AND c.column_name  = fl.column_name))
),
single AS (
    SELECT focus_area_pk, MIN(dimension_pk) AS dimension_pk
      FROM candidates
     GROUP BY focus_area_pk
    HAVING COUNT(*) = 1
)
UPDATE "prism"."Focus_Areas" fa
   SET dimension_pk = s.dimension_pk
  FROM single s
 WHERE fa.focus_area_pk = s.focus_area_pk
   AND fa.dimension_pk IS NULL;

-- ---- 2. Drill pointers, by the area's dimension instead of its name -----------------------------
UPDATE "prism"."Focus_Levels" fl
   SET drill_to_focus_level_pk = target.focus_level_pk
  FROM (VALUES
    ('Sprint_Dim', 'Year',           'Year & Quarter'),
    ('Sprint_Dim', 'Year & Quarter', 'Year & Month'),
    ('Sprint_Dim', 'Year & Month',   'Year & Sprint'),
    ('Sprint_Dim', 'Quarter',        'Month'),
    ('Sprint_Dim', 'Month',          'Sprint'),
    ('Team_Dim',   'Level 5',        'Level 4'),
    ('Team_Dim',   'Level 4',        'Level 3'),
    ('Team_Dim',   'Level 3',        'Level 2'),
    ('Team_Dim',   'Level 2',        'Level 1'),
    ('Team_Dim',   'Level 1',        'Team')
  ) AS v(table_name, level, drill_to)
  JOIN "prism"."Dimensions" d        ON d.schema_name = 'prism' AND d.table_name = v.table_name
  JOIN "prism"."Focus_Areas" fa      ON fa.dimension_pk = d.dimension_pk
  JOIN "prism"."Focus_Levels" target ON target.focus_area_pk = fa.focus_area_pk AND target.name = v.drill_to
 WHERE fl.focus_area_pk = fa.focus_area_pk
   AND fl.name = v.level
   AND fl.drill_to_focus_level_pk IS NULL;

-- ---- 3. Required once every area has one ---------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "prism"."Focus_Areas" WHERE dimension_pk IS NULL) THEN
    ALTER TABLE "prism"."Focus_Areas" ALTER COLUMN dimension_pk SET NOT NULL;
  ELSE
    RAISE WARNING 'Focus_Areas still without a dimension_pk (no single dimension holds all their level columns): %',
      (SELECT string_agg(name, ', ') FROM "prism"."Focus_Areas" WHERE dimension_pk IS NULL);
  END IF;
END$$;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('017-focus_areas_link_by_columns.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
