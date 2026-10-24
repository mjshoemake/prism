package com.prism.api.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * The Advisor's guides: owner-curated Markdown under {@code src/main/resources/advisor/guides/}, versioned in git
 * (docs/MCP.md §4.1). Read from the classpath on first use and kept in memory.
 */
@Component
public class AdvisorGuides {

    private static final Logger log = LogCategory.SERVICE.logger(AdvisorGuides.class);

    /** One guide: {@code id} is the file name without {@code .md} and the last part of its {@code prism://guide/} URI. */
    public record Guide(String id, String title, String description) {

        public String uri() {
            return "prism://guide/" + id;
        }
    }

    /** Every guide the Advisor offers. Add the Markdown file and a line here (and an @McpResource method). */
    public static final List<Guide> GUIDES = List.of(
            new Guide("metric-types", "Prism metric types",
                    "How Prism calculates metrics: the four types (percentage, ratio, average, sum), row conditions, "
                            + "why ratios are a ratio of sums and never an average of percentages, percentiles, and "
                            + "how to choose a type."),
            new Guide("star-schema", "Designing Prism-compatible fact and dimension tables",
                    "How to shape fact and dimension tables for Prism: grain, counts instead of stored ratios, "
                            + "time and organization dimensions, working practices as dimensions, labels and NULLs, "
                            + "and a checklist."),
            new Guide("drill-down", "Focus areas, focus levels and drill-down",
                    "How Prism slices and drills: focus areas on dimensions, focus levels as columns, display vs "
                            + "drill order, standalone levels, filters carried across areas, and label gotchas."),
            new Guide("improvement-paths", "Improvement paths: it's never just one thing",
                    "Ordered improvement paths instead of single best practices: foundation vs conditional "
                            + "practices, the quality path (test quality, coverage, continuous integration, batch "
                            + "size), readiness checks, test-quality measures and traps such as gamed targets."));

    private final Map<String, String> texts = new ConcurrentHashMap<>();

    public Guide guide(String id) {
        log.debug("Started");
        return GUIDES.stream().filter(g -> g.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown guide: " + id));
    }

    /** The guide's Markdown text. */
    public String text(String id) {
        log.debug("Started");
        guide(id); // only known guides, never an arbitrary classpath path
        return texts.computeIfAbsent(id, AdvisorGuides::load);
    }

    private static String load(String id) {
        log.debug("Started");
        var path = "/advisor/guides/" + id + ".md";
        try (InputStream in = AdvisorGuides.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Guide file missing from the classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read guide " + path, e);
        }
    }
}
