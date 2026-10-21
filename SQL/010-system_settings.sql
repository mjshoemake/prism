-- 010-system_settings.sql
--
-- 1. Enterprise-specific system settings: exactly one row per enterprise.
--
--      enterprise_pk              PK and FK -> Enterprises (so at most one row per enterprise)
--      temp_password_valid_hours  how long a generated temporary password can be used to sign in
--                                 (default 72; 1..720). Changed under Admin -> System Settings.
--
--    Every existing enterprise gets a row; a trigger creates one for each new enterprise.
--
-- 2. Users.temp_password_expires_at: when the user's temporary password stops working. Set by the API
--    when it generates a temporary password (new user, password reset); cleared when the user chooses
--    their own password. Existing users still on a temporary password get now() + 72 hours.
--
--    Temporary passwords are generated per user by the API and shown once to the administrator; only
--    their hash is stored. Enterprises.temp_password_hash (a shared default) is no longer used.
--
-- 3. Realigns key sequences (end of script): earlier scripts inserted rows with explicit keys
--    (Enterprises in 007, Focus_Areas by hand, Sprint_Dim/Team_Dim demo data in 002/003), which never
--    advances a SERIAL/BIGSERIAL/IDENTITY sequence, so the next insert without a key would collide.
--
-- RE-RUNNABLE: guarded DDL; existing settings are never overwritten; sequences only ever move forward.
-- Also upgrades an earlier draft of this script (default_temp_password_hash column), if that was run.

-- ---- System_Settings ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "prism"."System_Settings" (
  enterprise_pk              BIGINT      PRIMARY KEY,
  temp_password_valid_hours  INTEGER     NOT NULL DEFAULT 72,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Draft-version upgrade: add the new column, drop the never-used shared-password hash.
ALTER TABLE "prism"."System_Settings"
  ADD COLUMN IF NOT EXISTS temp_password_valid_hours INTEGER NOT NULL DEFAULT 72;
ALTER TABLE "prism"."System_Settings" DROP COLUMN IF EXISTS default_temp_password_hash;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'system_settings_temp_password_valid_hours_ck') THEN
    ALTER TABLE "prism"."System_Settings"
      ADD CONSTRAINT system_settings_temp_password_valid_hours_ck
      CHECK (temp_password_valid_hours BETWEEN 1 AND 720);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'system_settings_enterprise_fk') THEN
    ALTER TABLE "prism"."System_Settings"
      ADD CONSTRAINT system_settings_enterprise_fk
      FOREIGN KEY (enterprise_pk) REFERENCES "prism"."Enterprises" (enterprise_pk)
      ON UPDATE RESTRICT ON DELETE CASCADE;
  END IF;
END$$;

-- updated_at maintenance (prism.set_updated_at() is created by 006/007).
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgname = 'system_settings_set_updated_at'
      AND tgrelid = '"prism"."System_Settings"'::regclass
  ) THEN
    CREATE TRIGGER system_settings_set_updated_at
      BEFORE UPDATE ON "prism"."System_Settings"
      FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at();
  END IF;
END$$;

-- One row (with the defaults) for every existing enterprise.
INSERT INTO "prism"."System_Settings" (enterprise_pk)
SELECT e.enterprise_pk FROM "prism"."Enterprises" e
ON CONFLICT (enterprise_pk) DO NOTHING;

-- Every new enterprise gets its settings row.
CREATE OR REPLACE FUNCTION "prism".create_default_system_settings() RETURNS trigger AS $$
BEGIN
  INSERT INTO "prism"."System_Settings" (enterprise_pk)
  VALUES (NEW.enterprise_pk)
  ON CONFLICT (enterprise_pk) DO NOTHING;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgname = 'enterprises_create_system_settings'
      AND tgrelid = '"prism"."Enterprises"'::regclass
  ) THEN
    CREATE TRIGGER enterprises_create_system_settings
      AFTER INSERT ON "prism"."Enterprises"
      FOR EACH ROW EXECUTE FUNCTION "prism".create_default_system_settings();
  END IF;
END$$;

COMMENT ON TABLE "prism"."System_Settings" IS 'Enterprise-specific settings, one row per enterprise';
COMMENT ON COLUMN "prism"."System_Settings".temp_password_valid_hours IS
  'Hours a generated temporary password can be used to sign in (1..720)';

-- ---- Users.temp_password_expires_at -------------------------------------------------------------
ALTER TABLE "prism"."Users" ADD COLUMN IF NOT EXISTS temp_password_expires_at TIMESTAMPTZ NULL;

UPDATE "prism"."Users"
   SET temp_password_expires_at = now() + INTERVAL '72 hours'
 WHERE is_temp_password = TRUE
   AND temp_password_expires_at IS NULL;

COMMENT ON COLUMN "prism"."Users".temp_password_expires_at IS
  'When the temporary password stops working (NULL when the user has chosen their own password)';
COMMENT ON COLUMN "prism"."Enterprises".temp_password_hash IS
  'Unused: temporary passwords are generated per user (see SQL/010)';

-- ---- Key sequences ------------------------------------------------------------------------------
-- Focus_Areas' sequence was created separately in 001 and never tied to its column, so Postgres
-- doesn't know they belong together (pg_get_serial_sequence returns NULL). Tie them (re-runnable).
ALTER SEQUENCE IF EXISTS "prism"."Focus_Areas_focus_area_pk_seq"
  OWNED BY "prism"."Focus_Areas".focus_area_pk;

-- Move each sequence past the highest key in use. Never moves a sequence backwards.
DO $$
DECLARE
  t   RECORD;
  seq TEXT;
  max_key BIGINT;
  last_key BIGINT;
BEGIN
  FOR t IN SELECT * FROM (VALUES
      ('Enterprises',  'enterprise_pk'),
      ('Focus_Areas',  'focus_area_pk'),
      ('Focus_Levels', 'focus_level_pk'),
      ('Sprint_Dim',   'sprint_pk'),
      ('Team_Dim',     'team_pk')
    ) AS v(table_name, column_name)
  LOOP
    IF to_regclass(format('%I.%I', 'prism', t.table_name)) IS NULL THEN
      CONTINUE;
    END IF;
    seq := pg_get_serial_sequence(format('%I.%I', 'prism', t.table_name), t.column_name);
    IF seq IS NULL THEN
      RAISE NOTICE 'No sequence for prism.%.% - skipped', t.table_name, t.column_name;
      CONTINUE;
    END IF;

    EXECUTE format('SELECT MAX(%I) FROM %I.%I', t.column_name, 'prism', t.table_name) INTO max_key;
    SELECT last_value INTO last_key FROM pg_sequences
     WHERE format('%I.%I', schemaname, sequencename) = seq;

    IF max_key IS NOT NULL AND max_key > COALESCE(last_key, 0) THEN
      PERFORM setval(seq, max_key);  -- next value = max_key + 1
      RAISE NOTICE 'prism.%.%: sequence moved to %', t.table_name, t.column_name, max_key;
    END IF;
  END LOOP;
END$$;
