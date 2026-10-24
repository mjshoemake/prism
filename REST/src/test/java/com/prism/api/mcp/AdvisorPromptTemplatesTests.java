package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Plain JUnit: every prompt template loads, its placeholders match its arguments, it only names tools and resources
 * the Advisor really has, and arguments are checked and filled in safely.
 */
class AdvisorPromptTemplatesTests {

    /** The Advisor's tools (AdvisorTools). Update when a tool is added. */
    private static final Set<String> TOOLS = Set.of("search_metric_catalog", "draft_metric", "review_star_schema");

    private final AdvisorPromptTemplates templates = new AdvisorPromptTemplates();

    @Test
    void everyTemplateLoadsWithExactlyItsPlaceholders() {
        var placeholder = Pattern.compile("\\{\\{([a-z_]+)}}");
        for (var template : AdvisorPromptTemplates.TEMPLATES) {
            var found = new HashSet<String>();
            placeholder.matcher(templates.text(template.id())).results().forEach(m -> found.add(m.group(1)));
            var expected = template.arguments().stream().map(AdvisorPromptTemplates.Argument::name)
                    .collect(Collectors.toSet());
            assertEquals(expected, found, template.id() + " placeholders");
        }
    }

    @Test
    void templatesNameOnlyRealToolsAndResources() {
        var guideUris = AdvisorGuides.GUIDES.stream().map(AdvisorGuides.Guide::uri).collect(Collectors.toSet());
        var promptIds = AdvisorPromptTemplates.TEMPLATES.stream().map(AdvisorPromptTemplates.Template::id)
                .collect(Collectors.toSet());
        var code = Pattern.compile("`([^`]+)`");
        for (var template : AdvisorPromptTemplates.TEMPLATES) {
            var text = templates.text(template.id());
            code.matcher(text).results().map(m -> m.group(1)).forEach(name -> {
                if (name.startsWith("prism://guide/")) {
                    assertTrue(guideUris.contains(name), template.id() + " names an unknown guide: " + name);
                } else if (name.startsWith("prism://")) {
                    assertTrue(name.equals("prism://catalog/metrics") || name.equals("prism://catalog/metrics/{id}"),
                            template.id() + " names an unknown resource: " + name);
                } else if (name.matches("[a-z]+(_[a-z]+)+")) {
                    assertTrue(TOOLS.contains(name) || name.startsWith("level_"),
                            template.id() + " names an unknown tool: " + name);
                } else if (name.matches("[a-z]+(-[a-z]+)+")) {
                    assertTrue(promptIds.contains(name), template.id() + " names an unknown prompt: " + name);
                }
            });
        }
    }

    @Test
    void renderFillsInTheArgument() {
        var text = templates.render("choose-metrics", Map.of("struggle", "  too many failed deployments \n"));
        assertTrue(text.contains("<struggle>\ntoo many failed deployments\n</struggle>"));
        assertFalse(text.contains("{{"));
        assertTrue(text.contains("search_metric_catalog"));
    }

    @Test
    void reviewSchemaKeepsTheDdlExactly() {
        var ddl = "CREATE TABLE \"prism\".\"Team_Dim\" (team_pk SERIAL PRIMARY KEY, note TEXT DEFAULT '$1 \\\\ x');";
        var text = templates.render("review-schema", Map.of("ddl", ddl));
        assertTrue(text.contains("<ddl>\n" + ddl + "\n</ddl>"));
        assertTrue(text.contains("review_star_schema"));
    }

    @Test
    void argumentTextIsNeverTreatedAsAPlaceholder() {
        var text = templates.render("design-star-schema", Map.of("description", "sprints {{description}} {{ddl}}"));
        assertTrue(text.contains("sprints {{description}} {{ddl}}"));
    }

    @Test
    void missingOrBlankArgumentsAreRefused() {
        var missing = assertThrows(IllegalArgumentException.class, () -> templates.render("review-schema", Map.of()));
        assertTrue(missing.getMessage().contains("'ddl'"));
        assertThrows(IllegalArgumentException.class,
                () -> templates.render("choose-metrics", Map.of("struggle", "   ")));
    }

    @Test
    void argumentsOverTheLimitAreRefused() {
        var longText = "x".repeat(AdvisorPromptTemplates.MAX_TEXT + 1);
        var error = assertThrows(IllegalArgumentException.class,
                () -> templates.render("choose-metrics", Map.of("struggle", longText)));
        assertTrue(error.getMessage().contains("4000"));
        templates.render("review-schema", Map.of("ddl", "x".repeat(StarSchemaReviewService.MAX_DDL)));
        assertThrows(IllegalArgumentException.class,
                () -> templates.render("review-schema", Map.of("ddl", "x".repeat(StarSchemaReviewService.MAX_DDL + 1))));
    }

    @Test
    void unknownPromptIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> templates.text("../application.properties"));
        assertThrows(IllegalArgumentException.class, () -> templates.render("nope", Map.of()));
    }
}
