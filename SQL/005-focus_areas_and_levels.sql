DROP TABLE IF EXISTS prism."Focus_Areas";
--CREATE SEQUENCE prism."Focus_Areas_focus_area_pk_seq";
CREATE TABLE IF NOT EXISTS prism."Focus_Areas"
(
    focus_area_pk integer NOT NULL DEFAULT nextval('prism."Focus_Areas_focus_area_pk_seq"'::regclass),
    name character varying(30) COLLATE pg_catalog."default" NOT NULL,
    column_name character varying(30) NOT NULL,
    CONSTRAINT "Focus_Areas_pkey" PRIMARY KEY (focus_area_pk)
);

ALTER TABLE IF EXISTS prism."Focus_Levels"
    OWNER to postgres;

-- Seed the two demo focus areas (corrected Oct 2026: they were originally entered by hand, so a fresh database
-- failed at 009, which adds a foreign key from Focus_Levels to Focus_Areas).
INSERT INTO prism."Focus_Areas" (focus_area_pk, name, column_name)
VALUES (1, 'Time', ''), (2, 'Organization', '')
ON CONFLICT DO NOTHING;
