-- 014-db_scripts_executed.sql
--
-- A record of which migration scripts have run on this database, so it's easy to check:
--   SELECT * FROM "prism"."DB_Scripts_Executed" ORDER BY name;
--
--   name           the script's file name, e.g. '013-data_sources_facts_dimensions.sql'
--   date_executed  when it last completed; NULL = it ran before this table existed (001–013), date not known
--
-- From 014 on, every script ends with one INSERT into this table (see the end of this file), so the row is only
-- written when everything above it succeeded. Run scripts with psql -v ON_ERROR_STOP=1 (setup.sh does): without
-- it, psql carries on after an error and would still record the script. A re-run updates date_executed.
--
-- Backfill: 001–012 are recorded as run (the API requires them; all have run on every Prism database so far).
-- 013 is recorded only if its tables exist. Earlier scripts are not edited (they are historical).
--
-- RE-RUNNABLE: CREATE ... IF NOT EXISTS; backfilled rows are only added when missing.

CREATE TABLE IF NOT EXISTS "prism"."DB_Scripts_Executed" (
  name           VARCHAR(100) NOT NULL,
  date_executed  TIMESTAMPTZ,
  CONSTRAINT "DB_Scripts_Executed_pkey" PRIMARY KEY (name)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'db_scripts_executed_name_ck') THEN
    ALTER TABLE "prism"."DB_Scripts_Executed" ADD CONSTRAINT db_scripts_executed_name_ck
      CHECK (name ~ '^[0-9]{3}-[A-Za-z0-9_-]+\.sql$');
  END IF;
END$$;

COMMENT ON TABLE "prism"."DB_Scripts_Executed" IS
  'Migration scripts in prism/SQL that have completed on this database; each script (014+) adds its own row last';
COMMENT ON COLUMN "prism"."DB_Scripts_Executed".date_executed IS
  'When the script last completed; NULL = run before this table existed (001–013), date unknown';

-- ---- Backfill ----------------------------------------------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
SELECT v.name, NULL
  FROM (VALUES
    ('001-create_star_schema.sql'),
    ('002-insert_sprint_dim_data_complete.sql'),
    ('003-insert_team_dim_data_complete.sql'),
    ('004-insert_fact_table_complete.sql'),
    ('005-focus_areas_and_levels.sql'),
    ('006-create_security_tables.sql'),
    ('007-users-and-auth.sql'),
    ('008-metrics.sql'),
    ('009-focus_sort_order_and_keys.sql'),
    ('010-system_settings.sql'),
    ('011-users_blank_username_to_null.sql'),
    ('012-metric_types.sql')
  ) AS v(name)
ON CONFLICT (name) DO NOTHING;

-- 013 ran if its last table and its last trigger exist.
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
SELECT '013-data_sources_facts_dimensions.sql', NULL
 WHERE to_regclass('"prism"."Facts_Dimensions_Assoc"') IS NOT NULL
   AND EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dimensions_set_updated_at')
ON CONFLICT (name) DO NOTHING;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('014-db_scripts_executed.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
