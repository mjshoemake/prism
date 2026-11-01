package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Metric parity check (permanent version; {@code tools/parity/README.md}). Runs chart requests through the real
 * {@link SprintMetricsService} — metadata cache, query builder and JDBC binding exactly as the API uses them — and
 * compares the results as text:
 * <ol>
 * <li><b>Hand-checked dataset</b>: {@code tools/parity/fixture/expected.tsv} against the {@code prism_fixture}
 *     database built by {@code fixture.sh}.</li>
 * <li><b>Demo data</b>: every metric × focus level × filter combination against {@code playpen}, compared with the
 *     frozen baseline {@code tools/parity/baseline/demo-baseline.txt}.</li>
 * <li><b>Check the check</b>: a deliberately changed calculation must be reported as different.</li>
 * </ol>
 * Needs live databases, so it only runs when asked: {@code tools/parity/run.sh} (or
 * {@code -Dprism.parity=true}; add {@code -Dprism.parity.freeze=true} to write a new baseline).
 * Connection: {@code spring.datasource.*} from application.properties; override with {@code prism.parity.url},
 * {@code prism.parity.fixture-url}, {@code prism.parity.username}, {@code prism.parity.password}.
 */
@EnabledIfSystemProperty(named = "prism.parity", matches = "true")
class MetricParityTests {

    static final Path PARITY_DIR = Path.of("tools", "parity");
    static final Path EXPECTED = PARITY_DIR.resolve("fixture").resolve("expected.tsv");
    static final Path BASELINE = PARITY_DIR.resolve("baseline").resolve("demo-baseline.txt");
    static final Path DEMO_ACTUAL = Path.of("target", "parity", "demo-actual.txt");
    static final Path FIXTURE_ACTUAL = Path.of("target", "parity", "fixture-actual.txt");

    /** Same seed every run, so the random filter combinations are the same cases each time. */
    static final long RANDOM_SEED = 20261021L;
    static final int RANDOM_CASES_PER_METRIC = 300;

    private static Database demo;
    private static Database fixture;
    private static Object savedRootLevel;

    @BeforeAll
    static void connect() throws IOException {
        savedRootLevel = setRootLogLevel("WARN"); // the per-method "Started" debug lines would flood the console
        var app = new Properties();
        try (InputStream in = MetricParityTests.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                app.load(in);
            }
        }
        var url = System.getProperty("prism.parity.url", app.getProperty("spring.datasource.url"));
        var user = System.getProperty("prism.parity.username", app.getProperty("spring.datasource.username"));
        // Same sources as application.properties' ${PRISM_DB_PASSWORD:${PGPASSWORD:}} (a plain Properties load doesn't
        // resolve placeholders).
        var password = System.getProperty("prism.parity.password", firstNonBlank(System.getenv("PRISM_DB_PASSWORD"),
                System.getenv("PGPASSWORD"), ""));
        var fixtureUrl = System.getProperty("prism.parity.fixture-url", url.replaceFirst("/[^/?]+(\\?|$)", "/prism_fixture$1"));
        demo = new Database(url, user, password);
        fixture = new Database(fixtureUrl, user, password);
    }

    private static String firstNonBlank(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    @AfterAll
    static void disconnect() {
        if (demo != null) {
            demo.close();
        }
        if (fixture != null) {
            fixture.close();
        }
        if (savedRootLevel != null) {
            setRootLogLevel(savedRootLevel.toString());
        }
    }

    // ---- 1. Hand-checked dataset ----------------------------------------------------------------------------------

    @Test
    void fixtureMatchesHandCheckedAnswers() throws IOException {
        var expected = ParityResults.read(EXPECTED);
        assertFalse(expected.isEmpty(), EXPECTED + " has no cases");
        var actual = runExpectedCases(fixture, expected.keySet(), false);
        ParityResults.write(FIXTURE_ACTUAL, "Actual results for " + EXPECTED, actual);
        var comparison = ParityResults.compare(expected, actual);
        System.out.println(comparison.report("Hand-checked dataset"));
        assertTrue(comparison.identical(), comparison.report("Hand-checked dataset"));
    }

    // ---- 2. Demo data against the frozen baseline -----------------------------------------------------------------

    @Test
    void demoDataMatchesFrozenBaseline() throws IOException {
        var actual = new LinkedHashMap<String, List<String>>();
        for (var parityCase : generateCases(demo)) {
            actual.put(parityCase.key(), demo.run(parityCase, false));
        }
        ParityResults.write(DEMO_ACTUAL, "Actual results on " + demo.url, actual);

        if (Boolean.getBoolean("prism.parity.freeze")) {
            ParityResults.write(BASELINE, """
                    Frozen parity baseline: every metric x focus level x filter case on the demo data (%s).
                    Written by tools/parity/run.sh --freeze; compare future changes against it. Do not edit by hand.\
                    """.formatted(demo.url), actual);
            System.out.printf("Baseline frozen: %d cases, %d rows -> %s%n", actual.size(),
                    actual.values().stream().mapToInt(List::size).sum(), BASELINE);
            return;
        }
        if (!Files.exists(BASELINE)) {
            fail("No baseline yet (" + BASELINE + "). Freeze one with: tools/parity/run.sh --freeze");
        }
        var comparison = ParityResults.compare(ParityResults.read(BASELINE), actual);
        System.out.println(comparison.report("Demo data vs baseline"));
        assertTrue(comparison.identical(), comparison.report("Demo data vs baseline") + "\nFull results: " + DEMO_ACTUAL);
    }

    // ---- 3. Check the check ---------------------------------------------------------------------------------------

    /**
     * A wrong calculation ({@link Database#wrongCalculation}) must show up as differences for every metric, or the
     * comparison proves nothing.
     */
    @Test
    void comparisonDetectsACalculationChange() throws IOException {
        var expected = ParityResults.read(EXPECTED);
        var mutated = runExpectedCases(fixture, expected.keySet(), true);
        var comparison = ParityResults.compare(expected, mutated);
        System.out.println(comparison.report("Wrong calculation (differences expected), hand-checked dataset", 0));
        for (var header : expected.keySet()) {
            var metric = ParityCase.splitUnescaped(header, " | ").get(1);
            assertTrue(comparison.differing().stream().anyMatch(key -> ParityCase.splitUnescaped(key, " | ").get(1).equals(metric)),
                    "A wrong calculation of " + ParityCase.unescape(metric) + " was not detected on the hand-checked dataset");
        }

        if (Files.exists(BASELINE) && !Boolean.getBoolean("prism.parity.freeze")) {
            var baseline = ParityResults.read(BASELINE);
            var sample = new LinkedHashMap<String, List<String>>();
            var mutatedDemo = new LinkedHashMap<String, List<String>>();
            for (var key : baseline.keySet()) {
                var parityCase = ParityCase.parseKey(key);
                if (parityCase.filters().isEmpty()) { // the no-filter cases are enough to prove detection
                    sample.put(key, baseline.get(key));
                    mutatedDemo.put(key, demo.run(parityCase, true));
                }
            }
            var demoComparison = ParityResults.compare(sample, mutatedDemo);
            System.out.println(demoComparison.report("Wrong calculation (differences expected), demo data", 0));
            assertFalse(demoComparison.differing().isEmpty(), "A wrong calculation was not detected on the demo data");
        }
    }

    // ---- Cases ----------------------------------------------------------------------------------------------------

    /** Runs the cases of an expected file (headers "description | key"); results keyed by the same header. */
    private static Map<String, List<String>> runExpectedCases(Database db, Iterable<String> headers, boolean mutate) {
        var results = new LinkedHashMap<String, List<String>>();
        for (var header : headers) {
            var parts = ParityCase.splitUnescaped(header, " | ");
            if (parts.size() != 4) {
                throw new IllegalArgumentException(EXPECTED + ": a case header needs 'description | metric | level | filters': " + header);
            }
            results.put(header, db.run(ParityCase.parseKey(String.join(" | ", parts.subList(1, 4))), mutate));
        }
        return results;
    }

    /**
     * Every metric × focus level × filter combination worth checking, generated from the database (no hand-copied
     * metadata). Per active metric:
     * <ul>
     * <li>each focus level with no filter;</li>
     * <li>each focus level filtered by every single value of every focus level (NULL values included);</li>
     * <li>drill-down paths: each value, then the next level in the area by sort_order, 3 levels deep for the first
     *     metric and 2 for the others (plain filter combinations, so they stay valid when drill pointers arrive);</li>
     * <li>{@value #RANDOM_CASES_PER_METRIC} seeded random combinations of 2–3 filters;</li>
     * <li>a value with an apostrophe.</li>
     * </ul>
     * Filter values are the keys the metrics return (step 1e: a filter matches a level's key column; for a level
     * without one the key is its label), so the generator doesn't need to know the FROM clause. Results are still
     * compared as label|value: a wrong key shows up in the filtered cases.
     */
    static List<ParityCase> generateCases(Database db) {
        var metrics = db.cache.activeMetrics().stream().map(MetricDefinition::name).toList();
        var levels = db.levelsInOrder();
        var values = new LinkedHashMap<String, List<String>>();
        for (var level : levels) {
            var seen = new LinkedHashSet<String>();
            for (var metric : metrics) {
                for (var row : db.fetch(new ParityCase(metric, level.name(), List.of()))) {
                    var key = row.get("key");
                    seen.add(key == null ? null : key.toString());
                }
            }
            values.put(level.name(), new ArrayList<>(seen));
        }
        var nextInArea = new HashMap<String, String>();
        for (int i = 0; i + 1 < levels.size(); i++) {
            if (levels.get(i).areaPk() == levels.get(i + 1).areaPk()) {
                nextInArea.put(levels.get(i).name(), levels.get(i + 1).name());
            }
        }

        var cases = new LinkedHashMap<String, ParityCase>(); // by key: duplicates (e.g. random repeats) run once
        var random = new Random(RANDOM_SEED);
        var levelNames = levels.stream().map(Level::name).toList();
        for (int m = 0; m < metrics.size(); m++) {
            var metric = metrics.get(m);
            for (var level : levelNames) {
                add(cases, new ParityCase(metric, level, List.of()));
            }
            for (var level : levelNames) {
                for (var filterLevel : levelNames) {
                    for (var value : values.get(filterLevel)) {
                        add(cases, new ParityCase(metric, level, List.of()).withFilter(filterLevel, value));
                    }
                }
            }
            for (var level : levelNames) {
                drill(cases, new ParityCase(metric, level, List.of()), values, nextInArea, m == 0 ? 3 : 2);
            }
            var filterable = levelNames.stream().filter(level -> !values.get(level).isEmpty()).toList();
            for (int i = 0; i < RANDOM_CASES_PER_METRIC && filterable.size() >= 2; i++) {
                var pool = new ArrayList<>(filterable);
                var parityCase = new ParityCase(metric, levelNames.get(random.nextInt(levelNames.size())), List.of());
                int count = Math.min(pool.size(), 2 + random.nextInt(2));
                for (int f = 0; f < count; f++) {
                    var filterLevel = pool.remove(random.nextInt(pool.size()));
                    var options = values.get(filterLevel);
                    parityCase = parityCase.withFilter(filterLevel, options.get(random.nextInt(options.size())));
                }
                add(cases, parityCase);
            }
            add(cases, new ParityCase(metric, levelNames.get(0), List.of()).withFilter(levelNames.get(levelNames.size() - 1), "O'Brien"));
        }
        return List.copyOf(cases.values());
    }

    private static void drill(Map<String, ParityCase> cases, ParityCase parityCase, Map<String, List<String>> values,
                              Map<String, String> nextInArea, int depth) {
        add(cases, parityCase);
        var next = nextInArea.get(parityCase.level());
        if (depth == 0 || next == null) {
            return;
        }
        for (var value : values.get(parityCase.level())) {
            drill(cases, new ParityCase(parityCase.metric(), next, parityCase.filters()).withFilter(parityCase.level(), value),
                    values, nextInArea, depth - 1);
        }
    }

    private static void add(Map<String, ParityCase> cases, ParityCase parityCase) {
        cases.putIfAbsent(parityCase.key(), parityCase);
    }

    // ---- Database -------------------------------------------------------------------------------------------------

    record Level(int pk, int areaPk, String name, int areaSort, int sort) {
    }

    /** One database with the API's own metadata cache and service on top of it. */
    static final class Database {
        private static final String LEVELS_SQL = """
                SELECT fl.focus_level_pk, fl.focus_area_pk, fl.name, fa.sort_order AS area_sort, fl.sort_order
                FROM "prism"."Focus_Levels" fl
                JOIN "prism"."Focus_Areas" fa ON fa.focus_area_pk = fl.focus_area_pk
                """;

        final String url;
        private final SingleConnectionDataSource dataSource;
        private final JdbcTemplate jdbc;
        final MetricsMetadataCache cache;
        private final SprintMetricsService service;
        private List<Level> levels;

        Database(String url, String user, String password) {
            this.url = url;
            this.dataSource = new SingleConnectionDataSource(url, user, password, true);
            this.jdbc = new JdbcTemplate(dataSource);
            this.cache = new MetricsMetadataCache(jdbc, Duration.ofHours(1));
            this.service = new SprintMetricsService(jdbc, cache,
                    // Prism's own login on purpose: parity compares calculations, not permissions (the live API runs
                    // them as prism_reader, SQL/022–023).
                    new MetricDataProviders(List.of(new PrismDatabaseDataProvider(MetricQueryConnection.unchecked(jdbc)))));
        }

        /** Focus levels in display order (area sort_order, then level sort_order). Names must be unique. */
        List<Level> levelsInOrder() {
            if (levels == null) {
                var loaded = new ArrayList<Level>();
                var names = new HashMap<String, Integer>();
                for (var row : jdbc.queryForList(LEVELS_SQL)) {
                    var level = new Level(number(row, "focus_level_pk"), number(row, "focus_area_pk"), (String) row.get("name"),
                            number(row, "area_sort"), number(row, "sort_order"));
                    if (names.put(level.name(), level.pk()) != null) {
                        throw new IllegalStateException("Focus level names must be unique for parity keys: " + level.name());
                    }
                    loaded.add(level);
                }
                loaded.sort(Comparator.comparingInt(Level::areaSort).thenComparingInt(Level::areaPk)
                        .thenComparingInt(Level::sort).thenComparingInt(Level::pk));
                levels = List.copyOf(loaded);
            }
            return levels;
        }

        private Level level(String name) {
            return levelsInOrder().stream().filter(l -> l.name().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown focus level in " + url + ": " + name));
        }

        private MetricDefinition metric(String name) {
            return cache.activeMetrics().stream().filter(m -> m.name().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive metric in " + url + ": " + name));
        }

        /** Chart rows exactly as the API returns them. */
        List<Map<String, Object>> fetch(ParityCase parityCase) {
            var filters = parityCase.filters().stream()
                    .map(f -> new ChartDataRequest.Filter(level(f.level()).pk(), "eq", f.value())).toList();
            return service.fetchChartData(new ChartDataRequest(metric(parityCase.metric()).metricPk(),
                    level(parityCase.level()).pk(), filters));
        }

        /**
         * The case's rows as text. {@code mutate}: the same case with {@link #wrongCalculation} instead of the metric's
         * real calculation, built and bound the way {@link SprintMetricsService#fetchChartData} does it.
         */
        List<String> run(ParityCase parityCase, boolean mutate) {
            List<Map<String, Object>> rows;
            if (!mutate) {
                rows = fetch(parityCase);
            } else {
                var changed = wrongCalculation(metric(parityCase.metric()));
                var filters = parityCase.filters().stream().map(f -> new MetricQueryBuilder.ResolvedFilter(
                        cache.focusLevel(level(f.level()).pk()).orElseThrow(), f.value())).toList();
                var query = MetricQueryBuilder.build(changed, cache.focusLevel(level(parityCase.level()).pk()).orElseThrow(), filters);
                Object[] args = query.parameters().stream().map(v -> new SqlParameterValue(Types.OTHER, v)).toArray();
                rows = jdbc.queryForList(query.sql(), args);
            }
            return rows.stream().map(row -> ParityResults.row(row.get("label"), row.get("value"))).toList();
        }

        void close() {
            dataSource.destroy();
        }

        /**
         * A plausible but wrong version of the metric, with the same decimals (so only the arithmetic differs):
         * percentage/ratio → the average of per-row ratios (the "average of averages" error, OVERVIEW §4.1);
         * average → missing values counted as zero; sum → one decimal fewer.
         */
        static MetricDefinition wrongCalculation(MetricDefinition m) {
            var type = m.type();
            var numerator = m.numeratorSql();
            var decimals = m.decimalPlaces();
            switch (m.type()) {
                case PERCENTAGE, RATIO -> {
                    numerator = "(" + m.numeratorSql() + ")::DECIMAL / NULLIF((" + m.denominatorSql() + ")::DECIMAL, 0)"
                            + (m.type() == MetricType.PERCENTAGE ? " * 100" : "");
                    type = MetricType.AVERAGE;
                }
                case AVERAGE -> numerator = "COALESCE((" + m.numeratorSql() + ")::DECIMAL, 0)";
                case SUM -> decimals--;
            }
            return new MetricDefinition(m.metricPk(), m.name(), type, numerator, null, m.rowConditionSql(), m.title(),
                    m.unit(), decimals, m.scaleMode(), m.scaleMin(), m.scaleMax(), m.active(), m.fact());
        }

        private static int number(Map<String, Object> row, String column) {
            return ((Number) row.get(column)).intValue();
        }
    }

    /**
     * Sets the Logback root level by reflection (no compile-time Logback dependency, like {@code LogFrames});
     * returns the previous level, or null with any other SLF4J backend.
     */
    private static Object setRootLogLevel(String levelName) {
        try {
            var root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            var levelClass = Class.forName("ch.qos.logback.classic.Level", true, root.getClass().getClassLoader());
            var previous = root.getClass().getMethod("getLevel").invoke(root);
            var level = levelClass.getMethod("toLevel", String.class).invoke(null, levelName);
            root.getClass().getMethod("setLevel", levelClass).invoke(root, level);
            return previous;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
