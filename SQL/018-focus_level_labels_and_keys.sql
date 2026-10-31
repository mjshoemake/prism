-- 018-focus_level_labels_and_keys.sql
--
-- Step 1e of the fact/dimension model (docs/ROADMAP.md): Time levels get display labels separate from their keys
-- (owner, 2026-10-31). The key groups, sorts and filters; the label is only what a bar shows.
--
--   Level           Label (shown)          Key (group / sort / filter)
--   Year            2024                   (label)
--   Year & Quarter  Q4 2024                year_quarter      ('2024 Q4')
--   Year & Month    Dec 2024               year_month        ('2024 12')
--   Year & Sprint   Sprint 26              sprint_start_date (only the sprint key carries the year)
--   Quarter         Q4                     sprint_quarter    (4)
--   Month           Dec                    sprint_month      (12)
--   Sprint          Sprint 26              sprint_num        (26: all years together)
--
-- 1. Demo Sprint_Dim: label columns generated from the existing numbers, so every row (and the parity fixture's
--    rows) has them without a data load: quarter_label, month_abbr, year_quarter_label, year_month_label.
-- 2. Focus_Levels of the area linked to Sprint_Dim: label column and key column as above. Only levels still on
--    their SQL/008–015 columns are changed, so a level an admin has edited is left alone.
--
-- Chart results change on purpose (bars in key order, labels as above, years kept apart where the key separates
-- them). The parity baseline is frozen again after the differences are agreed.
--
-- RE-RUNNABLE: ADD COLUMN IF NOT EXISTS; updates only touch levels still on their old columns.

-- ---- 1. Label columns on the demo sprint dimension ---------------------------------------------
ALTER TABLE "prism"."Sprint_Dim" ADD COLUMN IF NOT EXISTS quarter_label VARCHAR(2)
  GENERATED ALWAYS AS ('Q' || sprint_quarter::text) STORED;

ALTER TABLE "prism"."Sprint_Dim" ADD COLUMN IF NOT EXISTS month_abbr VARCHAR(3)
  GENERATED ALWAYS AS (CASE sprint_month
                         WHEN 1 THEN 'Jan' WHEN 2 THEN 'Feb' WHEN 3 THEN 'Mar' WHEN 4 THEN 'Apr'
                         WHEN 5 THEN 'May' WHEN 6 THEN 'Jun' WHEN 7 THEN 'Jul' WHEN 8 THEN 'Aug'
                         WHEN 9 THEN 'Sep' WHEN 10 THEN 'Oct' WHEN 11 THEN 'Nov' WHEN 12 THEN 'Dec'
                       END) STORED;

ALTER TABLE "prism"."Sprint_Dim" ADD COLUMN IF NOT EXISTS year_quarter_label VARCHAR(7)
  GENERATED ALWAYS AS ('Q' || sprint_quarter::text || ' ' || sprint_year::text) STORED;

ALTER TABLE "prism"."Sprint_Dim" ADD COLUMN IF NOT EXISTS year_month_label VARCHAR(8)
  GENERATED ALWAYS AS (CASE sprint_month
                         WHEN 1 THEN 'Jan' WHEN 2 THEN 'Feb' WHEN 3 THEN 'Mar' WHEN 4 THEN 'Apr'
                         WHEN 5 THEN 'May' WHEN 6 THEN 'Jun' WHEN 7 THEN 'Jul' WHEN 8 THEN 'Aug'
                         WHEN 9 THEN 'Sep' WHEN 10 THEN 'Oct' WHEN 11 THEN 'Nov' WHEN 12 THEN 'Dec'
                       END || ' ' || sprint_year::text) STORED;

COMMENT ON COLUMN "prism"."Sprint_Dim".quarter_label IS 'Display label, e.g. Q4 (generated, SQL/018)';
COMMENT ON COLUMN "prism"."Sprint_Dim".month_abbr IS 'Display label, e.g. Dec (generated, SQL/018)';
COMMENT ON COLUMN "prism"."Sprint_Dim".year_quarter_label IS 'Display label, e.g. Q4 2024 (generated, SQL/018)';
COMMENT ON COLUMN "prism"."Sprint_Dim".year_month_label IS 'Display label, e.g. Dec 2024 (generated, SQL/018)';

-- ---- 2. Label and key columns of the Time levels -------------------------------------------------
UPDATE "prism"."Focus_Levels" fl
   SET column_name     = v.label_column,
       key_column_name = v.key_column
  FROM (VALUES
    ('Year & Quarter', 'year_quarter',   'year_quarter_label', 'year_quarter'),
    ('Year & Month',   'year_month',     'year_month_label',   'year_month'),
    ('Year & Sprint',  'sprint_abbr',    'sprint_name',        'sprint_start_date'),
    ('Quarter',        'sprint_quarter', 'quarter_label',      'sprint_quarter'),
    ('Month',          'sprint_month',   'month_abbr',         'sprint_month'),
    ('Sprint',         'sprint_name',    'sprint_name',        'sprint_num')
  ) AS v(level, old_column, label_column, key_column)
  JOIN "prism"."Dimensions" d   ON d.schema_name = 'prism' AND d.table_name = 'Sprint_Dim'
  JOIN "prism"."Focus_Areas" fa ON fa.dimension_pk = d.dimension_pk
 WHERE fl.focus_area_pk = fa.focus_area_pk
   AND fl.name = v.level
   AND fl.column_name = v.old_column;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('018-focus_level_labels_and_keys.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
