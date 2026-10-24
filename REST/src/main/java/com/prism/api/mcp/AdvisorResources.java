package com.prism.api.mcp;

import java.util.List;

import org.slf4j.Logger;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

/**
 * MCP resources of the Prism Advisor (docs/MCP.md §4.1): read-only reference content an assistant can load.
 * Only active when the MCP server is on ({@code prism.mcp.enabled=true}).
 */
@Component
public class AdvisorResources {

    private static final Logger log = LogCategory.CONTROLLER.logger(AdvisorResources.class);
    private static final String MARKDOWN = "text/markdown";

    private final AdvisorGuides guides;
    private final MetricCatalog catalog;

    public AdvisorResources(AdvisorGuides guides, MetricCatalog catalog) {
        this.guides = guides;
        this.catalog = catalog;
    }

    @McpResource(uri = "prism://catalog/metrics", name = "Prism metric catalog",
            description = "Index of curated metrics: name, improvement level (foundation, conditional, outcome), "
                    + "Prism type, summary and the URI of each entry. Use search_metric_catalog to find entries "
                    + "for a problem.")
    public ReadResourceResult catalogIndex() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.catalogIndex",
                () -> markdown("prism://catalog/metrics", catalog.index()));
    }

    @McpResource(uri = "prism://catalog/metrics/{id}", name = "Prism metric catalog entry",
            description = "One curated metric: what it tells you, how Prism calculates it (type, numerator, "
                    + "denominator, condition), the data and grain it needs, pitfalls, prerequisites and where it "
                    + "fits the improvement path. The id comes from the catalog index or search_metric_catalog.")
    public ReadResourceResult catalogEntry(String id) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.catalogEntry", () -> {
            var entry = catalog.entry(id)
                    .orElseThrow(() -> new IllegalArgumentException("No catalog entry '" + id + "'. See prism://catalog/metrics."));
            return markdown(entry.uri(), entry.text());
        });
    }

    @McpResource(uri = "prism://guide/metric-types", name = "Prism metric types",
            description = "How Prism calculates metrics: the four types (percentage, ratio, average, sum), row "
                    + "conditions, why ratios are a ratio of sums and never an average of percentages, percentiles, "
                    + "and how to choose a type. Read this before drafting a metric.")
    public ReadResourceResult metricTypesGuide() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.metricTypesGuide", () -> guide("metric-types"));
    }

    @McpResource(uri = "prism://guide/star-schema", name = "Designing Prism-compatible fact and dimension tables",
            description = "How to shape fact and dimension tables for Prism: grain, counts instead of stored ratios, "
                    + "time and organization dimensions, working practices as dimensions, labels and NULLs, and a "
                    + "checklist. Read this before designing or reviewing a schema.")
    public ReadResourceResult starSchemaGuide() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.starSchemaGuide", () -> guide("star-schema"));
    }

    @McpResource(uri = "prism://guide/drill-down", name = "Focus areas, focus levels and drill-down",
            description = "How Prism slices and drills: focus areas on dimensions, focus levels as columns, display "
                    + "vs drill order, standalone levels, filters carried across areas, and label gotchas.")
    public ReadResourceResult drillDownGuide() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.drillDownGuide", () -> guide("drill-down"));
    }

    @McpResource(uri = "prism://guide/improvement-paths", name = "Improvement paths: it's never just one thing",
            description = "Ordered improvement paths instead of single best practices: foundation vs conditional "
                    + "practices, the quality path (test quality, coverage, continuous integration, batch size), "
                    + "readiness checks, test-quality measures and traps. Read this before recommending improvements.")
    public ReadResourceResult improvementPathsGuide() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.improvementPathsGuide", () -> guide("improvement-paths"));
    }

    private ReadResourceResult guide(String id) {
        log.debug("Started");
        return markdown(guides.guide(id).uri(), guides.text(id));
    }

    private static ReadResourceResult markdown(String uri, String text) {
        log.debug("Started");
        return new ReadResourceResult(List.of(new TextResourceContents(uri, MARKDOWN, text)));
    }
}
