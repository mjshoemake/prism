package com.prism.api.mcp;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * MCP prompts of the Prism Advisor (docs/MCP.md §4.3): guided workflows a user picks in their assistant. Each returns
 * one user message built from an owner-curated template ({@link AdvisorPromptTemplates}); the assistant then follows
 * it with the Advisor's tools and resources. Only active when the MCP server is on ({@code prism.mcp.enabled=true}).
 */
@Component
public class AdvisorPrompts {

    private static final Logger log = LogCategory.CONTROLLER.logger(AdvisorPrompts.class);

    private final AdvisorPromptTemplates templates;

    public AdvisorPrompts(AdvisorPromptTemplates templates) {
        this.templates = templates;
    }

    @McpPrompt(name = "choose-metrics", description = """
            Pick Prism metrics for a struggle: clarify the goal (outcome or behavior, count or rate), search the \
            metric catalog, propose metrics with the data they need, prerequisites and pitfalls, and check the \
            improvement path for missing threads.""")
    public GetPromptResult chooseMetrics(
            @McpArg(name = "struggle", description = "What the team or leader is struggling with, in plain words, "
                    + "e.g. 'sprints keep missing their commitments'.", required = true) String struggle) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorPrompts.chooseMetrics",
                () -> prompt("choose-metrics", "Choose metrics for a struggle", Map.of("struggle", orEmpty(struggle))));
    }

    @McpPrompt(name = "design-star-schema", description = """
            Design Prism-compatible fact and dimension tables from a description of the data a team has, check them \
            with review_star_schema, and iterate until there are no errors.""")
    public GetPromptResult designStarSchema(
            @McpArg(name = "description", description = "The data the team has and what it wants to measure, e.g. "
                    + "'Jira sprints per team, a Jenkins deployment log, an org chart with three levels'.",
                    required = true) String description) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorPrompts.designStarSchema",
                () -> prompt("design-star-schema", "Design fact and dimension tables",
                        Map.of("description", orEmpty(description))));
    }

    @McpPrompt(name = "review-schema", description = """
            Review existing CREATE TABLE statements before connecting them to Prism: run review_star_schema, explain \
            each finding with its fix, and propose focus levels.""")
    public GetPromptResult reviewSchema(
            @McpArg(name = "ddl", description = "CREATE TABLE / ALTER TABLE statements, at most 64 KB. DDL only, "
                    + "never data.", required = true) String ddl) {
        log.debug("Started");
        return PerformanceLog.time("AdvisorPrompts.reviewSchema",
                () -> prompt("review-schema", "Review a schema before connecting it to Prism", Map.of("ddl", orEmpty(ddl))));
    }

    private GetPromptResult prompt(String id, String description, Map<String, String> arguments) {
        log.debug("Started");
        var text = templates.render(id, arguments);
        return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
    }

    /** {@code Map.of} refuses nulls; a missing argument becomes blank and is refused by the template with a clear message. */
    private static String orEmpty(String value) {
        log.debug("Started");
        return value == null ? "" : value;
    }
}
