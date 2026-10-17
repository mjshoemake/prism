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
    
    
    