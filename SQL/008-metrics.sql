CREATE TABLE prism."Metrics"
(
    metric_pk integer NOT NULL,
    name character varying(40) NOT NULL,
    valueColumnsForSelectClause character varying(200) NOT NULL,
    fromClause character varying(500) NOT NULL
);

ALTER TABLE IF EXISTS prism."Metrics"
    OWNER to postgres;

INSERT INTO "prism"."Metrics" 
(metric_pk, "name", valuecolumnsforselectclause, fromclause)
VALUES
(1, 
 'Say/Do Ratio', 
 'ROUND((SUM(smf.committed_story_points_completed)::DECIMAL / SUM(smf.committed_story_points)::DECIMAL) * 100, 2) as "Say/Do Ratio"',
 '"prism"."Sprint_Metrics_Fact" smf JOIN "prism"."Sprint_Dim" sd  ON smf.sprint_pk = sd.sprint_pk JOIN "prism"."Team_Dim" td ON smf.team_pk = td.team_pk');
 
INSERT INTO "prism"."Focus_Levels" 
(focus_area_pk, name, column_name)
VALUES
 (1,'Year','sprint_year'),
 (1,'Year & Quarter','year_quarter'),
 (1,'Year & Month','year_month'),
 (1,'Quarter','sprint_quarter'),
 (1,'Month','sprint_month'),
 (2,'Level 5','level_5_name'),
 (2,'Level 4','level_4_name'),
 (2,'Level 3','level_3_name'),
 (2,'Level 2','level_2_name'),
 (2,'Level 1','level_1_name'),
 (2,'Team','team_name'),
 (1,'Sprint','sprint_name');
