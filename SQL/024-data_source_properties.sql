-- 024-data_source_properties.sql
--
-- Name / value settings per data source (owner, 2026-11-01: a key/value table is more flexible than adding a column
-- for every connection setting), and a TEMPORARY development-only connection method.
--
--   Data_Source_Properties  one row per (data source, property name): e.g. username, password, sslmode,
--                           connectTimeout. is_secret marks values the API never returns (it only says one is set).
--                           Deleted with their data source. Which names are allowed, and which are passed to the
--                           database driver, is decided by the API (an allow-list: some driver settings could load
--                           code or turn off read-only, so arbitrary names are never passed on).
--
--   db_connection_method 'direct_dev'  TEMPORARY (owner, 2026-11-01): a direct connection (like firewall_allow_list)
--                           whose login comes from the properties 'username' and 'password' (secret) instead of a
--                           secrets store, so real database connections can be built and tested before the secrets
--                           store exists. Development only: never for a client's database. The API refuses it unless
--                           prism.data-sources.allow-dev-connections=true. Remove when the secrets store exists
--                           (ROADMAP): delete the direct_dev sources and drop the method from the CHECK below. Storing
--                           secret properties is allowed only for direct_dev.
--
-- Rules: direct_dev needs host and database name, no secret name (its login is in the properties; the API requires
-- username and password); other client methods need host, database name and secret name; prism_database nothing.
--
-- Plain SQL (psql, pgAdmin or any SQL tool). RE-RUNNABLE: table created only if missing, CHECKs replaced by name.

CREATE TABLE IF NOT EXISTS "prism"."Data_Source_Properties" (
  data_source_pk  INTEGER     NOT NULL,
  name            VARCHAR(63) NOT NULL,
  value           TEXT        NOT NULL,
  is_secret       BOOLEAN     NOT NULL DEFAULT false,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT "Data_Source_Properties_pkey" PRIMARY KEY (data_source_pk, name)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_source_properties_source_fk') THEN
    ALTER TABLE "prism"."Data_Source_Properties" ADD CONSTRAINT data_source_properties_source_fk
      FOREIGN KEY (data_source_pk) REFERENCES "prism"."Data_Sources" (data_source_pk)
      ON UPDATE RESTRICT ON DELETE CASCADE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_source_properties_name_ck') THEN
    ALTER TABLE "prism"."Data_Source_Properties" ADD CONSTRAINT data_source_properties_name_ck
      CHECK (name ~ '^[A-Za-z][A-Za-z0-9_.]*$');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'data_source_properties_set_updated_at'
                  AND tgrelid = '"prism"."Data_Source_Properties"'::regclass) THEN
    CREATE TRIGGER data_source_properties_set_updated_at BEFORE UPDATE ON "prism"."Data_Source_Properties"
      FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at();
  END IF;
END$$;

COMMENT ON TABLE "prism"."Data_Source_Properties" IS
  'Name/value settings per data source (username, sslmode, …); is_secret values are never returned by the API, SQL/024';
COMMENT ON COLUMN "prism"."Data_Source_Properties".is_secret IS
  'Never returned by the API. Secret values are stored here only for the TEMPORARY direct_dev method (development)';

ALTER TABLE "prism"."Data_Sources" DROP CONSTRAINT IF EXISTS data_sources_db_method_ck;
ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_db_method_ck
  CHECK (db_connection_method IS NULL
         OR db_connection_method IN ('prism_database', 'firewall_allow_list', 'private_link', 'outbound_agent',
                                     'direct_dev'));

ALTER TABLE "prism"."Data_Sources" DROP CONSTRAINT IF EXISTS data_sources_db_columns_ck;
ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_db_columns_ck
  CHECK (CASE
           WHEN db_connection_method IS NULL OR db_connection_method = 'prism_database'
             THEN db_host IS NULL AND db_port IS NULL AND db_name IS NULL AND db_credentials_ref IS NULL
           WHEN db_connection_method = 'direct_dev'
             THEN db_host IS NOT NULL AND db_name IS NOT NULL AND db_credentials_ref IS NULL
           ELSE db_host IS NOT NULL AND db_name IS NOT NULL AND db_credentials_ref IS NOT NULL
         END);

-- ---- Record this script (always the last statement) --------------------------------------------
INSERT INTO "prism"."DB_Scripts_Executed" (name, date_executed)
VALUES ('024-data_source_properties.sql', now())
ON CONFLICT (name) DO UPDATE SET date_executed = EXCLUDED.date_executed;
