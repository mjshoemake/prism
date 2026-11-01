-- 022-metric_query_reader.sql
--
-- Read-only login for metric queries (roadmap #3; docs/OVERVIEW.md §4.3): chart queries no longer run on Prism's own
-- login, which can read and change everything (users, password hashes, settings). They run on "prism_reader", which:
--   - can SELECT only the STAR tables registered in "Facts" / "Dimensions" for data sources with connection method
--     prism_database (the demo schema), nothing else in the schema (no Users, Metrics, …);
--   - starts every transaction read-only (default_transaction_read_only);
--   - has a statement time limit (statement_timeout 30s) and short lock / idle-in-transaction limits, set on the
--     login itself, so they hold whatever the client sends.
-- The API connects with it through a second connection pool (prism.metric-query.* in application.properties; password
-- from PRISM_READER_PASSWORD) and refuses chart queries if that login can read Prism's own tables.
--
-- PASSWORD: never in this file. After running it (from psql, pgAdmin or any SQL tool: plain SQL only), set it once:
--   ALTER ROLE prism_reader PASSWORD 'choose-one';
-- and put PRISM_READER_PASSWORD=choose-one in REST/.env.local (git-ignored). setup.sh does the ALTER ROLE itself when
-- PRISM_READER_PASSWORD is set.
--
-- NEW STAR TABLES: after registering a fact or dimension, run  SELECT "prism".grant_star_tables_to_reader();
-- (it revokes and re-grants, so the login can read exactly the registered tables). The metadata admin screens
-- (roadmap #5) will call it.
--
-- Needs a role allowed to create roles (e.g. postgres). Roles are cluster-wide: one prism_reader serves every
-- database on the server; the grants are per database.
-- RE-RUNNABLE: role created only if missing; settings and grants re-applied.

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'prism_reader') THEN
    CREATE ROLE prism_reader LOGIN;
  END IF;
END$$;

ALTER ROLE prism_reader WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
  CONNECTION LIMIT 20;
ALTER ROLE prism_reader SET default_transaction_read_only = on;
ALTER ROLE prism_reader SET statement_timeout = '30s';
ALTER ROLE prism_reader SET lock_timeout = '5s';
ALTER ROLE prism_reader SET idle_in_transaction_session_timeout = '60s';

-- SELECT on exactly the registered STAR tables of prism_database data sources; everything else in those schemas
-- (and in "prism") revoked. Missing tables are skipped with a warning. Returns the number of tables granted.
CREATE OR REPLACE FUNCTION "prism".grant_star_tables_to_reader() RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
  s       text;
  t       record;
  granted integer := 0;
BEGIN
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO prism_reader', current_database());

  FOR s IN
    SELECT 'prism'
    UNION
    SELECT x.schema_name
    FROM (SELECT f.schema_name, f.data_source_pk FROM "prism"."Facts" f
          UNION ALL
          SELECT d.schema_name, d.data_source_pk FROM "prism"."Dimensions" d) x
    JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = x.data_source_pk
    WHERE ds.db_connection_method = 'prism_database'
  LOOP
    IF to_regnamespace(quote_ident(s)) IS NOT NULL THEN
      EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM prism_reader', s);
      EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM prism_reader', s);
      EXECUTE format('GRANT USAGE ON SCHEMA %I TO prism_reader', s);
    END IF;
  END LOOP;

  FOR t IN
    SELECT DISTINCT x.schema_name, x.table_name
    FROM (SELECT f.schema_name, f.table_name, f.data_source_pk FROM "prism"."Facts" f
          UNION ALL
          SELECT d.schema_name, d.table_name, d.data_source_pk FROM "prism"."Dimensions" d) x
    JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = x.data_source_pk
    WHERE ds.db_connection_method = 'prism_database'
    ORDER BY 1, 2
  LOOP
    IF to_regclass(format('%I.%I', t.schema_name, t.table_name)) IS NULL THEN
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
  'Grants prism_reader SELECT on exactly the registered STAR tables (Facts/Dimensions of prism_database sources), SQL/022';

SELECT "prism".grant_star_tables_to_reader() AS star_tables_granted_to_prism_reader;

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('022-metric_query_reader.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
