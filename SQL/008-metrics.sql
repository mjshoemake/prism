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
