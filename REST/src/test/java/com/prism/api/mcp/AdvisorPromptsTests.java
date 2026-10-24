package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/** Plain JUnit: each prompt returns one user message with the argument filled in; a missing argument is refused. */
class AdvisorPromptsTests {

    private final AdvisorPrompts prompts = new AdvisorPrompts(new AdvisorPromptTemplates());

    @Test
    void eachPromptReturnsOneUserMessage() {
        List<GetPromptResult> results = List.of(
                prompts.chooseMetrics("sprints keep missing their commitments"),
                prompts.designStarSchema("Jira sprints per team and an org chart"),
                prompts.reviewSchema("CREATE TABLE team_dim (team_pk SERIAL PRIMARY KEY);"));
        for (var result : results) {
            assertEquals(1, result.messages().size());
            var message = result.messages().get(0);
            assertEquals(Role.USER, message.role());
            var text = ((TextContent) message.content()).text();
            assertTrue(text.contains("Prism Advisor"));
            assertFalse(result.description().isBlank());
        }
        var review = (TextContent) results.get(2).messages().get(0).content();
        assertTrue(review.text().contains("CREATE TABLE team_dim (team_pk SERIAL PRIMARY KEY);"));
    }

    @Test
    void missingArgumentIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> prompts.chooseMetrics(null));
        assertThrows(IllegalArgumentException.class, () -> prompts.reviewSchema(""));
    }
}
