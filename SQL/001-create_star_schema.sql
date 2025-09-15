-- PostgreSQL STAR Schema for Agile Scrum Metrics
-- Created for Power BI reporting

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS "prism";

-- Drop tables if they exist (in reverse dependency order)
DROP TABLE IF EXISTS "prism"."Sprint_Metrics_Fact";
DROP TABLE IF EXISTS "prism"."Sprint_Dim";
DROP TABLE IF EXISTS "prism"."Team_Dim";

-- Create Team Dimension Table
CREATE TABLE "prism"."Team_Dim"
(
    team_pk SERIAL PRIMARY KEY,
    team_name VARCHAR(45) NOT NULL,
    level_1_name VARCHAR(45),
    level_2_name VARCHAR(45),
    level_3_name VARCHAR(45),
    level_4_name VARCHAR(45),
    level_5_name VARCHAR(45),
    level_6_name VARCHAR(45),
    level_7_name VARCHAR(45)
);

ALTER TABLE "prism"."Team_Dim"
    OWNER TO postgres;

-- Create Sprint Dimension Table
CREATE TABLE "prism"."Sprint_Dim"
(
    sprint_pk SERIAL PRIMARY KEY,
    sprint_month INTEGER NOT NULL CHECK (sprint_month >= 1 AND sprint_month <= 12),
    sprint_year INTEGER NOT NULL CHECK (sprint_year >= 2000),
    sprint_quarter INTEGER NOT NULL CHECK (sprint_quarter >= 1 AND sprint_quarter <= 4),
    year_month VARCHAR(7) NOT NULL,
    year_quarter VARCHAR(10) NOT NULL,
    sprint_num INTEGER NOT NULL CHECK (sprint_num >= 1 AND sprint_num <= 100),
    sprint_name character varying(50) NOT NULL,
    sprint_abbr character varying(8) NOT NULL,
    sprint_start_date date NOT NULL,
    CONSTRAINT unique_year_sprint UNIQUE (sprint_year, sprint_num)
);

ALTER TABLE "prism"."Sprint_Dim"
    OWNER TO postgres;

-- Create Sprint Metrics Fact Table
CREATE TABLE "prism"."Sprint_Metrics_Fact"
(
    fact_pk SERIAL PRIMARY KEY,
    sprint_pk INTEGER NOT NULL,
    team_pk INTEGER NOT NULL,
    
    -- Say/Do Ratio components
    committed_story_points_completed INTEGER NOT NULL,
    committed_story_points INTEGER NOT NULL,
    
    -- Velocity components
    story_points_completed INTEGER NOT NULL,
    
    -- Commitment Strength components
    story_points_committed_at_start INTEGER NOT NULL,
    avg_velocity_3_sprints DECIMAL(10,2),
    avg_velocity_4_sprints DECIMAL(10,2),
    
    -- Scope Churn components
    story_points_added_during_sprint INTEGER NOT NULL DEFAULT 0,
    story_points_removed_during_sprint INTEGER NOT NULL DEFAULT 0,
    story_points_modified_scope INTEGER NOT NULL DEFAULT 0,
    
    -- Estimate Changed components
    stories_with_modified_estimate INTEGER NOT NULL DEFAULT 0,
    stories_completed INTEGER NOT NULL,
    
    -- % Estimated components
    stories_estimated INTEGER NOT NULL,
    total_stories INTEGER NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_sprint FOREIGN KEY (sprint_pk) REFERENCES "prism"."Sprint_Dim"(sprint_pk),
    CONSTRAINT fk_team FOREIGN KEY (team_pk) REFERENCES "prism"."Team_Dim"(team_pk),
    
    -- Unique constraint to prevent duplicate records
    CONSTRAINT unique_sprint_team UNIQUE (sprint_pk, team_pk)
);

ALTER TABLE "prism"."Sprint_Metrics_Fact"
    OWNER TO postgres;


-- Table: prism.Focus_Areas

-- DROP TABLE IF EXISTS prism."Focus_Areas";

CREATE TABLE IF NOT EXISTS prism."Focus_Areas"
(
    focus_area_pk integer NOT NULL DEFAULT nextval('prism."Focus_Areas_focus_area_pk_seq"'::regclass),
    name character varying(30) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT "Focus_Areas_pkey" PRIMARY KEY (focus_area_pk)
)

ALTER TABLE IF EXISTS prism."Focus_Areas"
    OWNER to postgres;


-- Table: prism.Focus_Levels

-- DROP TABLE IF EXISTS prism."Focus_Levels";

CREATE TABLE IF NOT EXISTS prism."Focus_Levels"
(
    focus_area_pk integer NOT NULL,
    name character varying(20) COLLATE pg_catalog."default" NOT NULL,
    column_name character varying(30) COLLATE pg_catalog."default" NOT NULL
)

ALTER TABLE IF EXISTS prism."Focus_Levels"
    OWNER to postgres;



-- Add comments for documentation
COMMENT ON TABLE "prism"."Team_Dim" IS 'Dimension table for teams with up to 7 hierarchical levels';
COMMENT ON TABLE "prism"."Sprint_Dim" IS 'Dimension table for sprints with time-based attributes';
COMMENT ON TABLE "prism"."Sprint_Metrics_Fact" IS 'Fact table containing Agile Scrum metrics as numerator/denominator pairs';

-- Create indexes for better query performance
CREATE INDEX idx_sprint_dim_year_month ON "prism"."Sprint_Dim" (sprint_year, sprint_month);
CREATE INDEX idx_sprint_dim_year_quarter ON "prism"."Sprint_Dim" (sprint_year, sprint_quarter);
CREATE INDEX idx_fact_sprint_pk ON "prism"."Sprint_Metrics_Fact" (sprint_pk);
CREATE INDEX idx_fact_team_pk ON "prism"."Sprint_Metrics_Fact" (team_pk);

-- Success message
SELECT 'STAR Schema created successfully!' as status;
