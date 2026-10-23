package com.prism.api.mcp;

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

    private final MetricDraftService metricDraft;

    public AdvisorTools(MetricDraftService metricDraft) {
        this.metricDraft = metricDraft;
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
