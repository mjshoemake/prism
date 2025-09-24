-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS "prism";

-- Table
CREATE TABLE IF NOT EXISTS "prism"."enterprises" (
  enterprise_pk BIGSERIAL PRIMARY KEY,
  name          TEXT        NOT NULL,
  url           TEXT        NOT NULL,
  auth_token    TEXT        NOT NULL,
  temp_password_hash TEXT   NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Optional: basic URL sanity (kept idempotent since it's inline). Comment/uncomment as needed.
-- ALTER TABLE "prism"."enterprises"
--   ADD CONSTRAINT enterprises_url_http_check CHECK (url ~* '^https?://')
--   NOT VALID;
-- -- NOTE: If you add the above later, run: VALIDATE CONSTRAINT enterprises_url_http_check;

-- Unique name (idempotent via index)
CREATE UNIQUE INDEX IF NOT EXISTS enterprises_name_uk_idx ON "prism"."enterprises" (name);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'enterprises_name_uk'
  ) THEN
    ALTER TABLE "prism"."enterprises"
      ADD CONSTRAINT enterprises_name_uk UNIQUE USING INDEX enterprises_name_uk_idx;
  END IF;
END$$;

-- updated_at trigger function (safe to re-run)
CREATE OR REPLACE FUNCTION "prism".set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgname = 'enterprises_set_updated_at'
      AND tgrelid = '"prism"."enterprises"'::regclass
  ) THEN
    DROP TRIGGER enterprises_set_updated_at ON "prism"."enterprises";
  END IF;

  CREATE TRIGGER enterprises_set_updated_at
  BEFORE UPDATE ON "prism"."enterprises"
  FOR EACH ROW EXECUTE FUNCTION "prism".set_updated_at();
END$$;