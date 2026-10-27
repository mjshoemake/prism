-- 013-data_sources_facts_dimensions.sql
--
-- First step of the fact/dimension model (docs/OVERVIEW.md §4.3, docs/ROADMAP.md §2 step 1): Prism's description of
-- where a STAR schema lives and how its tables join. Nothing reads these tables yet; the next steps point focus levels
-- (014) and metrics at them, and the query builder then writes its FROM / JOIN from them instead of the hard-coded
-- demo clause.
--
--   Data_Sources            where a STAR schema lives and how Prism connects; one enterprise each.
--                           Database connection columns (db_*) and API connection columns (api_*) are separate and
--                           exactly one of the two is filled. db_connection_method says how Prism reaches the
--                           database (OVERVIEW §4.3): firewall_allow_list | private_link | outbound_agent, or
--                           prism_database for the demo schema inside the Prism database (no host, no credentials).
--                           Credentials are never stored here: *_credentials_ref names the secret in the
--                           deployment's secrets store.
--   Facts                   fact tables of a data source: schema, table and the alias metric expressions use (smf).
--   Dimensions              dimension tables of a data source, shared by any number of facts.
--   Facts_Dimensions_Assoc  which dimensions a fact joins to, and on which columns. A fact and its dimensions must
--                           belong to the same data source (enforced by composite foreign keys).
--
-- Names: schema and table names are written into SQL quoted (so CamelCase works) and may not contain a double
-- quote; aliases and column names are written unquoted, so they must be plain identifiers (same rule as
-- MetricQueryBuilder). Aliases are unique per data source within each table. TODO (step 3): the metadata cache
-- must also refuse a fact and a linked dimension sharing an alias, which a constraint can't check across tables.
--
-- Seeds the demo schema as an ordinary data source of the 'Default' enterprise, reproducing exactly
-- MetricQueryBuilder.DEMO_FROM_CLAUSE:
--   "prism"."Sprint_Metrics_Fact" smf JOIN "prism"."Sprint_Dim" sd ON smf.sprint_pk = sd.sprint_pk
--                                     JOIN "prism"."Team_Dim"   td ON smf.team_pk   = td.team_pk
--
-- RE-RUNNABLE: CREATE ... IF NOT EXISTS, guarded constraints and triggers; seed rows are only added when missing.
-- Keys are generated (GENERATED ALWAYS), so seed data never inserts explicit keys.

-- ---- Data_Sources ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "prism"."Data_Sources" (
  data_source_pk        INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  enterprise_pk         BIGINT      NOT NULL,
  name                  VARCHAR(60) NOT NULL,
  description           TEXT,
  -- Database connection
  db_connection_method  TEXT,
  db_host               TEXT,
  db_port               INTEGER,
  db_name               TEXT,
  db_credentials_ref    TEXT,
  -- API connection (second phase, OVERVIEW §4.3: a client-run service implementing Prism's metric-data contract)
  api_url               TEXT,
  api_credentials_ref   TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_enterprise_fk') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_enterprise_fk
      FOREIGN KEY (enterprise_pk) REFERENCES "prism"."Enterprises" (enterprise_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_name_uk') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_name_uk UNIQUE (enterprise_pk, name);
  END IF;

  -- Exactly one of the two connections is filled.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_one_connection_ck') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_one_connection_ck
      CHECK ((db_connection_method IS NOT NULL) <> (api_url IS NOT NULL));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_db_method_ck') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_db_method_ck
      CHECK (db_connection_method IS NULL
             OR db_connection_method IN ('prism_database', 'firewall_allow_list', 'private_link', 'outbound_agent'));
  END IF;

  -- The Prism database needs no address or credentials; every other database needs host, name and credentials.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_db_columns_ck') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_db_columns_ck
      CHECK (CASE
               WHEN db_connection_method IS NULL OR db_connection_method = 'prism_database'
                 THEN db_host IS NULL AND db_port IS NULL AND db_name IS NULL AND db_credentials_ref IS NULL
               ELSE db_host IS NOT NULL AND db_name IS NOT NULL AND db_credentials_ref IS NOT NULL
             END);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_db_port_ck') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_db_port_ck
      CHECK (db_port IS NULL OR db_port BETWEEN 1 AND 65535);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_sources_api_columns_ck') THEN
    ALTER TABLE "prism"."Data_Sources" ADD CONSTRAINT data_sources_api_columns_ck
      CHECK ((api_url IS NULL) = (api_credentials_ref IS NULL)
             AND (api_url IS NULL OR api_url ~ '^https://'));
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS data_sources_enterprise_idx ON "prism"."Data_Sources" (enterprise_pk);

-- ---- Facts -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "prism"."Facts" (
  fact_pk          INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_source_pk   INTEGER     NOT NULL,
  name             VARCHAR(60) NOT NULL,
  description      TEXT,
  schema_name      VARCHAR(63) NOT NULL,
  table_name       VARCHAR(63) NOT NULL,
  table_alias      VARCHAR(30) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---- Dimensions --------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "prism"."Dimensions" (
  dimension_pk     INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_source_pk   INTEGER     NOT NULL,
  name             VARCHAR(60) NOT NULL,
  description      TEXT,
  schema_name      VARCHAR(63) NOT NULL,
  table_name       VARCHAR(63) NOT NULL,
  table_alias      VARCHAR(30) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Same keys and rules for both tables.
DO $$
DECLARE
  t TEXT;
  short TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY['Facts', 'Dimensions'] LOOP
    short := lower(t);

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_data_source_fk') THEN
      EXECUTE format('ALTER TABLE "prism".%I ADD CONSTRAINT %I FOREIGN KEY (data_source_pk) '
                     'REFERENCES "prism"."Data_Sources" (data_source_pk) ON UPDATE RESTRICT ON DELETE RESTRICT',
                     t, short || '_data_source_fk');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_name_uk') THEN
      EXECUTE format('ALTER TABLE "prism".%I ADD CONSTRAINT %I UNIQUE (data_source_pk, name)', t, short || '_name_uk');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_table_uk') THEN
      EXECUTE format('ALTER TABLE "prism".%I ADD CONSTRAINT %I UNIQUE (data_source_pk, schema_name, table_name)',
                     t, short || '_table_uk');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_alias_uk') THEN
      EXECUTE format('ALTER TABLE "prism".%I ADD CONSTRAINT %I UNIQUE (data_source_pk, table_alias)',
                     t, short || '_alias_uk');
    END IF;
    -- Lets Facts_Dimensions_Assoc require that a fact and its dimension share a data source.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_source_uk') THEN
      EXECUTE format('ALTER TABLE "prism".%I ADD CONSTRAINT %I UNIQUE (%I, data_source_pk)',
                     t, short || '_source_uk', CASE t WHEN 'Facts' THEN 'fact_pk' ELSE 'dimension_pk' END);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = short || '_names_ck') THEN
      EXECUTE format($f$ALTER TABLE "prism".%I ADD CONSTRAINT %I CHECK (
                        schema_name ~ '^[^"[:cntrl:]]+$'
                    AND table_name  ~ '^[^"[:cntrl:]]+$'
                    AND table_alias ~ '^[a-z_][a-z0-9_]*$')$f$, t, short || '_names_ck');
    END IF;
  END LOOP;
END$$;

-- ---- Facts_Dimensions_Assoc --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "prism"."Facts_Dimensions_Assoc" (
  fact_pk           INTEGER     NOT NULL,
  dimension_pk      INTEGER     NOT NULL,
  data_source_pk    INTEGER     NOT NULL,
  fact_column       VARCHAR(63) NOT NULL,
  dimension_column  VARCHAR(63) NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT "Facts_Dimensions_Assoc_pkey" PRIMARY KEY (fact_pk, dimension_pk)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facts_dimensions_assoc_fact_fk') THEN
    ALTER TABLE "prism"."Facts_Dimensions_Assoc" ADD CONSTRAINT facts_dimensions_assoc_fact_fk
      FOREIGN KEY (fact_pk, data_source_pk) REFERENCES "prism"."Facts" (fact_pk, data_source_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facts_dimensions_assoc_dimension_fk') THEN
    ALTER TABLE "prism"."Facts_Dimensions_Assoc" ADD CONSTRAINT facts_dimensions_assoc_dimension_fk
      FOREIGN KEY (dimension_pk, data_source_pk) REFERENCES "prism"."Dimensions" (dimension_pk, data_source_pk)
      ON UPDATE RESTRICT ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facts_dimensions_assoc_columns_ck') THEN
    ALTER TABLE "prism"."Facts_Dimensions_Assoc" ADD CONSTRAINT facts_dimensions_assoc_columns_ck
      CHECK (fact_column ~ '^[A-Za-z_][A-Za-z0-9_]*$' AND dimension_column ~ '^[A-Za-z_][A-Za-z0-9_]*$');
  END IF;
END$$;

-- "Dimensions of a fact" is the primary key; this supports "facts using a dimension".
CREATE INDEX IF NOT EXISTS facts_dimensions_assoc_dimension_idx ON "prism"."Facts_Dimensions_Assoc" (dimension_pk);

-- ---- updated_at triggers (prism.set_updated_at() from 006) -------------------------------------
DO $$
DECLARE
  t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY['Data_Sources', 'Facts', 'Dimensions'] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_trigger
       WHERE tgname = lower(t) || '_set_updated_at'
         AND tgrelid = format('%I.%I', 'prism', t)::regclass
    ) THEN
      EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE ON "prism".%I FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at()',
                     lower(t) || '_set_updated_at', t);
    END IF;
  END LOOP;
END$$;

-- ---- Comments ----------------------------------------------------------------------------------
COMMENT ON TABLE "prism"."Data_Sources" IS
  'Where a STAR schema lives and how Prism connects (OVERVIEW §4.3); exactly one of the db_* / api_* connections';
COMMENT ON COLUMN "prism"."Data_Sources".db_connection_method IS
  'prism_database (demo schema in the Prism database) | firewall_allow_list | private_link | outbound_agent';
COMMENT ON COLUMN "prism"."Data_Sources".db_credentials_ref IS
  'Name of the read-only login''s secret in the deployment''s secrets store; never the credentials themselves';
COMMENT ON COLUMN "prism"."Data_Sources".api_url IS 'Client-run metric-data service (second phase); https only';
COMMENT ON TABLE "prism"."Facts" IS 'Fact tables of a data source; metrics are calculated from one fact';
COMMENT ON COLUMN "prism"."Facts".table_alias IS 'Alias used in metric expressions, e.g. smf.committed_story_points';
COMMENT ON TABLE "prism"."Dimensions" IS 'Dimension tables of a data source, shared by any number of facts';
COMMENT ON TABLE "prism"."Facts_Dimensions_Assoc" IS
  'Which dimensions a fact joins to: fact_alias.fact_column = dimension_alias.dimension_column';

-- ---- Seed: the demo schema as a data source of the 'Default' enterprise --------------------------
INSERT INTO "prism"."Data_Sources" (enterprise_pk, name, description, db_connection_method)
SELECT e.enterprise_pk, 'Demo data', 'Demo STAR schema inside the Prism database (prototype only)', 'prism_database'
  FROM "prism"."Enterprises" e
 WHERE e.name = 'Default'
   AND NOT EXISTS (SELECT 1 FROM "prism"."Data_Sources" ds
                    WHERE ds.enterprise_pk = e.enterprise_pk AND ds.name = 'Demo data');

INSERT INTO "prism"."Facts" (data_source_pk, name, description, schema_name, table_name, table_alias)
SELECT ds.data_source_pk, 'Sprint metrics', 'One row per team per sprint', 'prism', 'Sprint_Metrics_Fact', 'smf'
  FROM "prism"."Data_Sources" ds
  JOIN "prism"."Enterprises" e ON e.enterprise_pk = ds.enterprise_pk
 WHERE e.name = 'Default' AND ds.name = 'Demo data'
ON CONFLICT DO NOTHING;

INSERT INTO "prism"."Dimensions" (data_source_pk, name, description, schema_name, table_name, table_alias)
SELECT ds.data_source_pk, v.name, v.description, 'prism', v.table_name, v.table_alias
  FROM "prism"."Data_Sources" ds
  JOIN "prism"."Enterprises" e ON e.enterprise_pk = ds.enterprise_pk
 CROSS JOIN (VALUES
    ('Sprint', 'Sprints with their month, quarter and year', 'Sprint_Dim', 'sd'),
    ('Team',   'Teams with up to 7 organization levels',     'Team_Dim',   'td')
  ) AS v(name, description, table_name, table_alias)
 WHERE e.name = 'Default' AND ds.name = 'Demo data'
ON CONFLICT DO NOTHING;

INSERT INTO "prism"."Facts_Dimensions_Assoc" (fact_pk, dimension_pk, data_source_pk, fact_column, dimension_column)
SELECT f.fact_pk, d.dimension_pk, f.data_source_pk, v.fact_column, v.dimension_column
  FROM (VALUES
    ('Sprint_Metrics_Fact', 'Sprint_Dim', 'sprint_pk', 'sprint_pk'),
    ('Sprint_Metrics_Fact', 'Team_Dim',   'team_pk',   'team_pk')
  ) AS v(fact_table, dimension_table, fact_column, dimension_column)
  JOIN "prism"."Data_Sources" ds ON ds.name = 'Demo data'
  JOIN "prism"."Enterprises" e   ON e.enterprise_pk = ds.enterprise_pk AND e.name = 'Default'
  JOIN "prism"."Facts" f         ON f.data_source_pk = ds.data_source_pk
                                AND f.schema_name = 'prism' AND f.table_name = v.fact_table
  JOIN "prism"."Dimensions" d    ON d.data_source_pk = ds.data_source_pk
                                AND d.schema_name = 'prism' AND d.table_name = v.dimension_table
ON CONFLICT DO NOTHING;
