-- Users and Auth (idempotent)
-- Schema and extensions
CREATE SCHEMA IF NOT EXISTS "prism";
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;    -- case-insensitive text type

-- Table: prism.users
CREATE TABLE IF NOT EXISTS "prism"."users" (
  user_id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  enterprise_pk         BIGINT      NOT NULL,
  email                 CITEXT      NOT NULL,
  username              CITEXT      NULL,
  password_hash         TEXT        NOT NULL,  -- Argon2id/bcrypt hash string from the app
  password_algo         TEXT        NOT NULL,  -- e.g., 'argon2id', 'bcrypt'
  password_params       JSONB       NOT NULL,  -- e.g., { "memory": 65536, "iterations": 3, "parallelism": 1 }
  is_temp_password      BOOLEAN     NOT NULL DEFAULT TRUE,
  password_updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_active             BOOLEAN     NOT NULL DEFAULT TRUE,
  failed_login_count    INTEGER     NOT NULL DEFAULT 0,
  locked_until          TIMESTAMPTZ NULL,
  last_login_at         TIMESTAMPTZ NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Unique constraint on email (idempotent via index + attach)
CREATE UNIQUE INDEX IF NOT EXISTS prism_users_email_uk_idx ON "prism"."users" (email);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'prism_users_email_uk'
  ) THEN
    ALTER TABLE "prism"."users"
      ADD CONSTRAINT prism_users_email_uk UNIQUE USING INDEX prism_users_email_uk_idx;
  END IF;
END$$;

-- updated_at trigger function (reusable across tables) in prism schema
CREATE OR REPLACE FUNCTION "prism".set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Ensure trigger is present on prism.users (drop/recreate safely)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgname = 'users_set_updated_at'
      AND tgrelid = '"prism"."users"'::regclass
  ) THEN
    DROP TRIGGER users_set_updated_at ON "prism"."users";
  END IF;

  CREATE TRIGGER users_set_updated_at
  BEFORE UPDATE ON "prism"."users"
  FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at();
END$$;

-- Optional helpful indexes (re-runnable)
-- Lookups by username
CREATE INDEX IF NOT EXISTS prism_users_username_idx ON "prism"."users" (username);
-- Active users filtering
CREATE INDEX IF NOT EXISTS prism_users_is_active_idx ON "prism"."users" (is_active);
-- Enterprise filter
CREATE INDEX IF NOT EXISTS prism_users_enterprise_idx ON "prism"."users" (enterprise_pk);

-- When updating password, is_temp_password should be set to false by the app. For safety,
-- you can add a helper function to force it when an explicit flag is missing (optional):
-- CREATE OR REPLACE FUNCTION "prism".unset_temp_password() RETURNS trigger AS $$
-- BEGIN
--   NEW.is_temp_password := FALSE;
--   NEW.password_updated_at := now();
--   RETURN NEW;
-- END; $$ LANGUAGE plpgsql;
-- DO $$ BEGIN
--   IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='users_unset_temp_password' AND tgrelid='"prism"."users"'::regclass) THEN
--     CREATE TRIGGER users_unset_temp_password BEFORE UPDATE OF password_hash ON "prism"."users"
--     FOR EACH ROW EXECUTE FUNCTION "prism".unset_temp_password();
--   END IF;
-- END $$;

-- Ensure FK to prism.enterprises exists (idempotent)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'users_enterprise_fk'
      AND table_schema = 'prism'
      AND table_name = 'users'
  ) THEN
    ALTER TABLE "prism"."users"
      ADD CONSTRAINT users_enterprise_fk
      FOREIGN KEY (enterprise_pk)
      REFERENCES "prism"."enterprises" (enterprise_pk)
      ON UPDATE RESTRICT
      ON DELETE RESTRICT;
  END IF;
END$$;


