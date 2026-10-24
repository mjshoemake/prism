package com.prism.api.mcp;

import java.util.List;

import org.slf4j.Logger;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * MCP tools of the Prism Advisor (docs/MCP.md §4.2). The logic lives in plain services; these methods only describe
 * the tools to the assistant and time each call. Only active when the MCP server is on ({@code prism.mcp.enabled=true}).
 */
@Component
public class AdvisorTools {

    private static final Logger log = LogCategory.CONTROLLER.logger(AdvisorTools.class);

    /** Longest problem description accepted (docs/MCP.md §4.4). */
    static final int MAX_PROBLEM = 4000;

    private final MetricDraftService metricDraft;
    private final MetricCatalog catalog;
    private final StarSchemaReviewService schemaReview;

    public AdvisorTools(MetricDraftService metricDraft, MetricCatalog catalog, StarSchemaReviewService schemaReview) {
        this.metricDraft = metricDraft;
        this.catalog = catalog;
        this.schemaReview = schemaReview;
    }

    @McpTool(name = "review_star_schema", description = """
            Review the CREATE TABLE (and ALTER TABLE) statements of a fact/dimension schema against Prism's \
            STAR-schema rules. Returns which tables look like facts or dimensions, each fact's grain, findings \
            (errors first; rules S1 grain, S2 stored ratios, S3 measures, S4 keys, S5 focus levels, S6 empty labels, \
            S7 time, S8 per-item grain; S0 could not read) with a suggestion for each, and suggested focus areas with \
            their levels in drill order. Paste DDL only, never data. The DDL is only parsed: it is never run, \
            stored or logged. Read prism://guide/star-schema to explain the findings.""")
    public SchemaReview reviewStarSchema(
            @McpToolParam(description = "CREATE TABLE / ALTER TABLE statements, at most 64 KB. Other statements "
                    + "(indexes, comments, grants) are skipped.", required = true) String ddl) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorTools.reviewStarSchema", () -> schemaReview.review(ddl));
    }

    @McpTool(name = "search_metric_catalog", description = """
            Find curated metrics for a problem a team or leader describes, e.g. "too many failed deployments" or \
            "sprints keep missing commitments". Returns up to 5 entries, best first, each with its improvement level \
            (foundation, conditional, outcome), Prism type, summary and URI. Read an entry's URI for how to \
            calculate it, the data it needs and its pitfalls, and prism://guide/improvement-paths before \
            recommending changes: it's never just one thing.""")
    public List<MetricCatalog.Hit> searchMetricCatalog(
            @McpToolParam(description = "The problem or goal in plain words.", required = true) String problem) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorTools.searchMetricCatalog", () -> {
            if (problem != null && problem.length() > MAX_PROBLEM) {
                throw new IllegalArgumentException("The problem description is longer than " + MAX_PROBLEM + " characters.");
            }
            return catalog.search(problem);
        });
    }

    @McpTool(name = "draft_metric", description = """
            Turn a metric idea into a Prism metric definition. Checks it against Prism's rules, returns errors and \
            warnings (e.g. a stored percentage used where raw counts are needed), a ready-to-run INSERT for Prism's \
            Metrics table, and the exact SQL Prism would build for it. Read prism://guide/metric-types first. \
            Expressions use the fact table alias, e.g. smf.committed_story_points. Never runs anything against a \
            database.""")
    public MetricDraft draftMetric(
            @McpToolParam(description = "Unique metric name, at most 40 characters, e.g. 'Change Failure Rate'.",
                    required = true) String name,
            @McpToolParam(description = "Metric type: percentage, ratio, average or sum.", required = true) String type,
            @McpToolParam(description = "Numerator for percentage/ratio, or the value for average/sum, e.g. "
                    + "'smf.failed_deployments'.", required = true) String numerator,
            @McpToolParam(description = "Denominator, for percentage and ratio only, e.g. 'smf.deployments'.",
                    required = false) String denominator,
            @McpToolParam(description = "Optional row condition applied to every part of the calculation, e.g. "
                    + "\"smf.environment = 'production'\".", required = false) String rowCondition,
            @McpToolParam(description = "Chart title, at most 60 characters (default: the name).", required = false)
            String title,
            @McpToolParam(description = "Unit shown with values, at most 20 characters, e.g. '%' or 'points'.",
                    required = false) String unit,
            @McpToolParam(description = "Decimal places, 0 to 6 (default 2).", required = false) Integer decimals) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorTools.draftMetric",
                () -> metricDraft.draft(name, type, numerator, denominator, rowCondition, title, unit, decimals));
    }
}
