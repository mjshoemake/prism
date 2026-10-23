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

    public AdvisorResources(AdvisorGuides guides) {
        this.guides = guides;
    }

    @McpResource(uri = "prism://guide/metric-types", name = "Prism metric types",
            description = "How Prism calculates metrics: the four types (percentage, ratio, average, sum), row "
                    + "conditions, why ratios are a ratio of sums and never an average of percentages, percentiles, "
                    + "and how to choose a type. Read this before drafting a metric.")
    public ReadResourceResult metricTypesGuide() {
        log.debug("Started");
        return PerformanceLog.time("AdvisorResources.metricTypesGuide", () -> guide("metric-types"));
    }

    private ReadResourceResult guide(String id) {
        log.debug("Started");
        var guide = guides.guide(id);
        return new ReadResourceResult(List.of(new TextResourceContents(guide.uri(), MARKDOWN, guides.text(id))));
    }
}
