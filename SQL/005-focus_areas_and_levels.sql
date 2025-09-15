CREATE TABLE prism."Focus_Levels"
(
    focus_area_pk integer NOT NULL,
    name character varying(20) NOT NULL,
    column_name character varying(30) NOT NULL
);

ALTER TABLE IF EXISTS prism."Focus_Levels"
    OWNER to postgres;