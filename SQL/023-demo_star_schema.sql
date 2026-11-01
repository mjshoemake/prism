-- 023-demo_star_schema.sql
--
-- The demo STAR tables leave Prism's schema (owner, 2026-11-01). Only the fact and dimension tables are the
-- customer's STAR schema; every metadata table is Prism's. They must not depend on each other, because a customer's
-- STAR schema lives outside Prism. So:
--   - "Sprint_Metrics_Fact", "Sprint_Dim" and "Team_Dim" move from "prism" to the new schema "demo_star" with their
--     data, keys, indexes, sequences and generated columns (ALTER TABLE … SET SCHEMA: no copy);
--   - the metadata ("Facts" / "Dimensions".schema_name) points at "demo_star"; Prism finds the tables only by these
--     names, so chart queries return exactly the same results;
--   - the two logins are confined to their own schema: Prism's login owns "prism"; the read-only metric login
--     prism_reader (SQL/022) gets SELECT on the registered STAR tables and nothing at all in "prism" (no USAGE, so it
--     can't even see that Prism's tables exist). The API refuses chart queries if prism_reader can use "prism".
--
-- Scripts 001–019 stay as they were (history): on a new database they still create the demo tables in "prism" first,
-- and this script moves them, so new and existing databases end the same. From here on, no Prism migration creates,
-- changes or loads a STAR table; demo data changes go to "demo_star" only.
--
-- RE-RUNNABLE: each table is moved only while it is still in "prism"; the grants are re-applied.

CREATE SCHEMA IF NOT EXISTS demo_star;
COMMENT ON SCHEMA demo_star IS 'Demo STAR schema (fact and dimension tables only), read by Prism through its metadata, SQL/023';

DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY['Sprint_Dim', 'Team_Dim', 'Sprint_Metrics_Fact'] LOOP
    IF to_regclass(format('%I.%I', 'prism', t)) IS NOT NULL THEN
      IF to_regclass(format('%I.%I', 'demo_star', t)) IS NOT NULL THEN
        RAISE EXCEPTION 'Both prism.% and demo_star.% exist; resolve by hand before running 023', t, t;
      END IF;
      EXECUTE format('ALTER TABLE %I.%I SET SCHEMA demo_star', 'prism', t);
    END IF;
  END LOOP;
END$$;

UPDATE "prism"."Facts"
   SET schema_name = 'demo_star'
 WHERE schema_name = 'prism' AND table_name = 'Sprint_Metrics_Fact';
UPDATE "prism"."Dimensions"
   SET schema_name = 'demo_star'
 WHERE schema_name = 'prism' AND table_name IN ('Sprint_Dim', 'Team_Dim');

-- Grants (replaces SQL/022's version): prism_reader may read exactly the registered STAR tables of prism_database
-- sources and has no privilege at all in "prism". A registered table inside "prism" is never granted (warning):
-- Prism's schema holds only Prism's own tables.
CREATE OR REPLACE FUNCTION "prism".grant_star_tables_to_reader() RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
  s       text;
  t       record;
  granted integer := 0;
BEGIN
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO prism_reader', current_database());

  -- Nothing in Prism's own schema.
  REVOKE ALL ON ALL TABLES IN SCHEMA "prism" FROM prism_reader;
  REVOKE ALL ON ALL SEQUENCES IN SCHEMA "prism" FROM prism_reader;
  REVOKE ALL ON ALL FUNCTIONS IN SCHEMA "prism" FROM prism_reader;
  REVOKE ALL ON SCHEMA "prism" FROM prism_reader;

  -- Each STAR schema: start from nothing, then USAGE.
  FOR s IN
    SELECT DISTINCT x.schema_name
    FROM (SELECT f.schema_name, f.data_source_pk FROM "prism"."Facts" f
          UNION ALL
          SELECT d.schema_name, d.data_source_pk FROM "prism"."Dimensions" d) x
    JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = x.data_source_pk
    WHERE ds.db_connection_method = 'prism_database' AND x.schema_name <> 'prism'
  LOOP
    IF to_regnamespace(quote_ident(s)) IS NULL THEN
      RAISE WARNING 'prism_reader: registered schema % does not exist', s;
    ELSE
      EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM prism_reader', s);
      EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM prism_reader', s);
      EXECUTE format('GRANT USAGE ON SCHEMA %I TO prism_reader', s);
    END IF;
  END LOOP;

  -- SELECT on each registered table.
  FOR t IN
    SELECT DISTINCT x.schema_name, x.table_name
    FROM (SELECT f.schema_name, f.table_name, f.data_source_pk FROM "prism"."Facts" f
          UNION ALL
          SELECT d.schema_name, d.table_name, d.data_source_pk FROM "prism"."Dimensions" d) x
    JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = x.data_source_pk
    WHERE ds.db_connection_method = 'prism_database'
    ORDER BY 1, 2
  LOOP
    IF t.schema_name = 'prism' THEN
      RAISE WARNING 'prism_reader: %.% is in Prism''s own schema; STAR tables must live in their own schema; not granted',
        t.schema_name, t.table_name;
    ELSIF to_regclass(format('%I.%I', t.schema_name, t.table_name)) IS NULL THEN
      RAISE WARNING 'prism_reader: registered table %.% does not exist; not granted', t.schema_name, t.table_name;
    ELSE
      EXECUTE format('GRANT SELECT ON %I.%I TO prism_reader', t.schema_name, t.table_name);
      granted := granted + 1;
    END IF;
  END LOOP;
  RETURN granted;
END$$;

REVOKE ALL ON FUNCTION "prism".grant_star_tables_to_reader() FROM PUBLIC;
COMMENT ON FUNCTION "prism".grant_star_tables_to_reader() IS
  'Grants prism_reader SELECT on exactly the registered STAR tables (Facts/Dimensions of prism_database sources, '
  'outside "prism") and nothing in "prism", SQL/022 + SQL/023';

SELECT "prism".grant_star_tables_to_reader() AS star_tables_granted_to_prism_reader;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('023-demo_star_schema.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
