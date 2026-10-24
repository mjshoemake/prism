package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prism.api.mcp.SchemaReview.Finding;

/** Plain JUnit: {@code review_star_schema} reading DDL, classifying tables, and rules S0–S8. */
class StarSchemaReviewServiceTests {

    private final StarSchemaReviewService service = new StarSchemaReviewService();

    /** The demo schema (SQL/001), with the script noise around it that people will paste too. */
    static final String DEMO = """
            CREATE SCHEMA IF NOT EXISTS "prism";
            DROP TABLE IF EXISTS "prism"."Sprint_Metrics_Fact";
            CREATE TABLE "prism"."Team_Dim" (
                team_pk SERIAL PRIMARY KEY,
                team_name VARCHAR(45) NOT NULL,
                level_1_name VARCHAR(45), level_2_name VARCHAR(45), level_3_name VARCHAR(45),
                level_4_name VARCHAR(45), level_5_name VARCHAR(45)
            );
            ALTER TABLE "prism"."Team_Dim" OWNER TO postgres;
            CREATE TABLE "prism"."Sprint_Dim" (
                sprint_pk SERIAL PRIMARY KEY,
                sprint_month INTEGER NOT NULL CHECK (sprint_month >= 1 AND sprint_month <= 12),
                sprint_year INTEGER NOT NULL CHECK (sprint_year >= 2000),
                sprint_quarter INTEGER NOT NULL,
                year_month VARCHAR(7) NOT NULL,
                year_quarter VARCHAR(10) NOT NULL,
                sprint_num INTEGER NOT NULL,
                sprint_name character varying(50) NOT NULL,
                sprint_start_date date NOT NULL,
                CONSTRAINT unique_year_sprint UNIQUE (sprint_year, sprint_num)
            );
            -- Say/Do; and a semicolon in a comment
            CREATE TABLE "prism"."Sprint_Metrics_Fact" (
                fact_pk SERIAL PRIMARY KEY,
                sprint_pk INTEGER NOT NULL,
                team_pk INTEGER NOT NULL,
                committed_story_points_completed INTEGER NOT NULL,
                committed_story_points INTEGER NOT NULL,
                story_points_added_during_sprint INTEGER NOT NULL DEFAULT 0,
                CONSTRAINT fk_sprint FOREIGN KEY (sprint_pk) REFERENCES "prism"."Sprint_Dim"(sprint_pk),
                CONSTRAINT fk_team FOREIGN KEY (team_pk) REFERENCES "prism"."Team_Dim"(team_pk),
                CONSTRAINT unique_sprint_team UNIQUE (sprint_pk, team_pk)
            );
            COMMENT ON TABLE "prism"."Team_Dim" IS 'Teams; up to 5 levels';
            CREATE INDEX idx_fact_team_pk ON "prism"."Sprint_Metrics_Fact" (team_pk);
            DO $$ BEGIN RAISE NOTICE 'a;b'; END $$;
            """;

    private static final String DIMS = """
            CREATE TABLE dim_team (team_id INT PRIMARY KEY, team_name TEXT NOT NULL UNIQUE, division TEXT NOT NULL);
            CREATE TABLE dim_sprint (sprint_id INT PRIMARY KEY, sprint_year INT NOT NULL, year_month CHAR(7) NOT NULL);
            """;

    private static List<Finding> rule(SchemaReview review, String rule) {
        return review.findings().stream().filter(f -> f.rule().equals(rule)).toList();
    }

    private static List<Finding> severity(SchemaReview review, String severity) {
        return review.findings().stream().filter(f -> f.severity().equals(severity)).toList();
    }

    private static boolean has(SchemaReview review, String severity, String rule, String column) {
        return review.findings().stream().anyMatch(f -> f.severity().equals(severity) && f.rule().equals(rule)
                && (column == null || (f.column() != null && f.column().contains(column))));
    }

    @Test
    void demoSchemaIsPrismCompatible() {
        var review = service.review(DEMO);
        assertTrue(severity(review, "error").isEmpty(), review.findings().toString());
        assertTrue(review.summary().contains("3 tables read (1 fact, 2 dimension)"), review.summary());
        assertTrue(review.summary().contains("Prism-compatible"), review.summary());

        var fact = review.tables().stream().filter(t -> t.name().equals("Sprint_Metrics_Fact")).findFirst().orElseThrow();
        assertEquals("fact", fact.role());
        assertEquals("one row per sprint_pk × team_pk", fact.grain());
        assertTrue(has(review, "note", "S1", "sprint_pk"));
        assertTrue(rule(review, "S1").get(0).suggestion().contains("N/A"), "gaps are N/A, never zero");
        assertTrue(rule(review, "S2").isEmpty(), "counts, not ratios");
        assertTrue(rule(review, "S7").isEmpty(), "Sprint_Dim is a time dimension");

        assertEquals(List.of("CREATE SCHEMA IF", "DROP TABLE IF", "COMMENT ON TABLE", "CREATE INDEX idx_fact_team_pk",
                "DO $$ BEGIN"), review.skipped().stream().filter(s -> !s.startsWith("ALTER")).toList());
    }

    @Test
    void demoFocusLevelsMatchPrism() {
        var review = service.review(DEMO);
        var org = review.focusAreas().stream().filter(f -> f.focusArea().equals("Organization")).findFirst().orElseThrow();
        assertEquals(List.of("level_5_name", "level_4_name", "level_3_name", "level_2_name", "level_1_name", "team_name"),
                org.drillLevels());
        var time = review.focusAreas().stream().filter(f -> f.focusArea().equals("Time")).findFirst().orElseThrow();
        assertEquals(List.of("sprint_year", "year_quarter", "year_month", "sprint_name"), time.drillLevels());
        assertEquals(List.of("sprint_quarter", "sprint_month"), time.standaloneLevels());
        assertTrue(has(review, "note", "S5", "sprint_name"), "sprint names repeat across years");
        assertTrue(has(review, "note", "S6", "level_1_name"), "nullable levels");
        assertEquals(1, rule(review, "S6").size(), "one note for all nullable levels");
    }

    @Test
    void surrogateKeyOnlyIsAGrainError() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_sprint (
                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  team_id INT NOT NULL REFERENCES dim_team (team_id),
                  sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                  points_done INT NOT NULL
                );""");
        var s1 = rule(review, "S1");
        assertEquals(1, s1.size(), s1.toString());
        assertEquals("error", s1.get(0).severity());
        assertTrue(s1.get(0).message().contains("generated id"), s1.get(0).message());
        assertTrue(s1.get(0).suggestion().contains("UNIQUE (team_id, sprint_id)"), s1.get(0).suggestion());
        assertTrue(review.summary().contains("Not Prism-compatible"), review.summary());
    }

    @Test
    void uniqueKeyAddedByAlterTableIsTheGrain() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_sprint (team_id INT NOT NULL, sprint_id INT NOT NULL, points_done INT NOT NULL);
                ALTER TABLE fact_sprint ADD CONSTRAINT fk_t FOREIGN KEY (team_id) REFERENCES dim_team (team_id);
                ALTER TABLE fact_sprint ADD CONSTRAINT fk_s FOREIGN KEY (sprint_id) REFERENCES dim_sprint (sprint_id);
                ALTER TABLE fact_sprint ADD PRIMARY KEY (team_id, sprint_id);""");
        assertTrue(severity(review, "error").isEmpty(), review.findings().toString());
        assertTrue(rule(review, "S4").isEmpty(), "foreign keys from ALTER TABLE count: " + review.findings());
        assertTrue(has(review, "note", "S1", "team_id"));
    }

    @Test
    void itemLevelFactHasItsOwnGrain() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_deployment (
                  deployment_ref VARCHAR(40) PRIMARY KEY,
                  team_id INT NOT NULL REFERENCES dim_team (team_id),
                  sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                  failed INT NOT NULL DEFAULT 0,
                  lead_time_hours NUMERIC(8,2) NOT NULL
                );""");
        assertTrue(severity(review, "error").isEmpty(), review.findings().toString());
        var fact = review.tables().stream().filter(t -> t.name().equals("fact_deployment")).findFirst().orElseThrow();
        assertEquals("one row per deployment_ref (item level)", fact.grain());
        assertTrue(rule(review, "S8").isEmpty(), "durations are fine per item");
        assertTrue(rule(review, "S3").isEmpty(), "text key isn't a descriptive column: " + review.findings());
    }

    @Test
    void uniqueOnPartOfTheKeysIsNotEnough() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_sprint (
                  team_id INT NOT NULL REFERENCES dim_team (team_id),
                  sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                  points_done INT NOT NULL,
                  UNIQUE (team_id)
                );""");
        assertTrue(has(review, "error", "S1", "team_id"), review.findings().toString());
    }

    @Test
    void storedRatiosAndSummaryDurationsAreFlagged() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_sprint (
                  team_id INT NOT NULL REFERENCES dim_team (team_id),
                  sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                  say_do_ratio NUMERIC(5,2),
                  failure_rate_pct DECIMAL(5,2),
                  avg_cycle_days NUMERIC,
                  deployments INT,
                  is_production BOOLEAN NOT NULL,
                  UNIQUE (team_id, sprint_id)
                );""");
        assertTrue(has(review, "warning", "S2", "say_do_ratio"));
        assertTrue(has(review, "warning", "S2", "failure_rate_pct"));
        assertTrue(has(review, "warning", "S2", "avg_cycle_days"));
        assertTrue(has(review, "note", "S8", "avg_cycle_days"), "duration on a summary row");
        assertTrue(has(review, "note", "S3", "deployments"), "nullable measure");
        assertFalse(has(review, "note", "S3", "say_do_ratio"), "one finding per column: S2 already covers it");
        assertTrue(has(review, "note", "S3", "is_production"), "non-numeric column");
    }

    @Test
    void keysWithoutDimensionsAreFlagged() {
        var review = service.review("""
                CREATE TABLE dim_team (team_id INT PRIMARY KEY, team_name TEXT NOT NULL UNIQUE);
                CREATE TABLE fact_week (
                  team_id INT NOT NULL,
                  week_id INT NOT NULL,
                  product_id INT NOT NULL REFERENCES dim_product (product_id),
                  deployments INT NOT NULL,
                  UNIQUE (team_id, week_id, product_id)
                );""");
        assertTrue(has(review, "warning", "S4", "team_id"), "implied link: declare the FK");
        assertTrue(rule(review, "S4").stream().anyMatch(f -> f.message().contains("no foreign key is declared")));
        assertTrue(has(review, "warning", "S4", "week_id"), "key with no dimension");
        assertTrue(has(review, "note", "S4", "product_id"), "dimension not in the DDL");
        assertTrue(has(review, "warning", "S7", null), "no time");
        assertTrue(has(review, "note", "S1", "week_id"), "grain includes all three keys: " + review.findings());
    }

    @Test
    void factWithoutDimensionsAndUnknownTables() {
        var review = service.review("""
                CREATE TABLE fact_orphan (value INT NOT NULL);
                CREATE TABLE settings (name TEXT, value TEXT);""");
        assertTrue(has(review, "error", "S1", null));
        assertTrue(review.tables().stream().anyMatch(t -> t.name().equals("settings") && t.role().equals("unknown")));
        assertTrue(rule(review, "S4").stream().anyMatch(f -> "settings".equals(f.table())));
    }

    @Test
    void dimensionWithoutPrimaryKeyAndGenericLevels() {
        var review = service.review("""
                CREATE TABLE dim_team (team_id INT NOT NULL, division TEXT NOT NULL, team_name TEXT NOT NULL,
                                       team_size INT);
                CREATE TABLE dim_sprint (sprint_id INT PRIMARY KEY, sprint_year INT NOT NULL);
                CREATE TABLE fact_x (team_id INT NOT NULL REFERENCES dim_team (team_id),
                                     sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                                     done INT NOT NULL, PRIMARY KEY (team_id, sprint_id));""");
        assertTrue(has(review, "warning", "S4", null) && rule(review, "S4").stream().anyMatch(f -> "dim_team".equals(f.table())));
        var team = review.focusAreas().stream().filter(f -> f.dimension().equals("dim_team")).findFirst().orElseThrow();
        assertEquals("Team", team.focusArea());
        assertEquals(List.of("division", "team_name"), team.drillLevels(), "broadest first, the team's own name last");
        assertTrue(has(review, "note", "S5", "team_name"), "label not unique");
    }

    @Test
    void unreadableAndEmptyInput() {
        var broken = service.review("CREATE TABLE fact_bad ( this is not sql ;");
        assertTrue(has(broken, "warning", "S0", null));
        assertEquals("fact_bad", rule(broken, "S0").stream().filter(f -> f.severity().equals("warning"))
                .findFirst().orElseThrow().table());
        assertTrue(has(broken, "error", "S0", null), "no tables read");
        assertTrue(broken.summary().startsWith("Nothing to review"), broken.summary());

        assertTrue(has(service.review("  "), "error", "S0", null));
        assertTrue(has(service.review(null), "error", "S0", null));
        var big = service.review("-- x\n".repeat(StarSchemaReviewService.MAX_DDL / 5 + 1));
        assertTrue(big.findings().get(0).message().contains("64 KB"), big.findings().toString());
    }

    @Test
    void findingsAreOrderedBySeverity() {
        var review = service.review(DIMS + """
                CREATE TABLE fact_sprint (id SERIAL PRIMARY KEY,
                  team_id INT NOT NULL REFERENCES dim_team (team_id),
                  sprint_id INT NOT NULL REFERENCES dim_sprint (sprint_id),
                  rate_pct NUMERIC, points INT);""");
        var ranks = review.findings().stream().map(f -> switch (f.severity()) {
            case "error" -> 0;
            case "warning" -> 1;
            default -> 2;
        }).toList();
        var sorted = ranks.stream().sorted().toList();
        assertEquals(sorted, ranks);
        assertEquals("error", review.findings().get(0).severity());
    }

    @Test
    void splitterRespectsQuotesDollarQuotingAndComments() {
        var statements = StarSchemaReviewService.splitStatements("""
                SELECT 'a;b'; -- c;d
                SELECT "x;y" /* e;f */ ;
                DO $body$ BEGIN PERFORM 1; END $body$;
                SELECT $$;$$""");
        assertEquals(4, statements.size(), statements.toString());
        assertEquals("SELECT 'a;b'", statements.get(0));
        assertTrue(statements.get(2).endsWith("$body$"), statements.get(2));
    }

    @Test
    void identifiersAreUnquotedAndSchemaStripped() {
        assertEquals("team_dim", StarSchemaReviewService.key("\"prism\".\"Team_Dim\""));
        assertEquals("Team_Dim", StarSchemaReviewService.unquote("prism.\"Team_Dim\""));
        assertEquals("a.b", StarSchemaReviewService.unquote("\"a.b\""));
    }
}
