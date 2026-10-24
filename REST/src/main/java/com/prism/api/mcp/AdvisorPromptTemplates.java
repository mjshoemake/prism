package com.prism.api.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * The Advisor's prompt templates: owner-curated Markdown under {@code src/main/resources/advisor/prompts/}, versioned
 * in git like the guides (docs/MCP.md §4.3). Each template holds {@code {{argument}}} placeholders that are filled in
 * one pass, so text a user passes in is never treated as a placeholder itself. Read on first use and kept in memory.
 */
@Component
public class AdvisorPromptTemplates {

    private static final Logger log = LogCategory.SERVICE.logger(AdvisorPromptTemplates.class);

    /** Longest free-text argument (docs/MCP.md §4.4). */
    static final int MAX_TEXT = 4000;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-z_]+)}}");

    /** One argument of a prompt: its name (also its placeholder) and its size limit. Required, like every argument. */
    public record Argument(String name, int maxLength) {
    }

    /**
     * One prompt: {@code id} is the MCP prompt name and the file name without {@code .md}. Descriptions shown to the
     * user live on the {@code @McpPrompt} methods in {@link AdvisorPrompts}.
     */
    public record Template(String id, List<Argument> arguments) {
    }

    /** Every prompt the Advisor offers. Add the Markdown file, a line here and an @McpPrompt method. */
    public static final List<Template> TEMPLATES = List.of(
            new Template("choose-metrics", List.of(new Argument("struggle", MAX_TEXT))),
            new Template("design-star-schema", List.of(new Argument("description", MAX_TEXT))),
            new Template("review-schema", List.of(new Argument("ddl", StarSchemaReviewService.MAX_DDL))));

    private final Map<String, String> texts = new ConcurrentHashMap<>();

    public Template template(String id) {
        log.debug("Started");
        return TEMPLATES.stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown prompt: " + id));
    }

    /** The template's raw Markdown, placeholders included. */
    public String text(String id) {
        log.debug("Started");
        template(id); // only known prompts, never an arbitrary classpath path
        return texts.computeIfAbsent(id, AdvisorPromptTemplates::load);
    }

    /**
     * The prompt text with every placeholder filled in. Each argument must be present, not blank and within its
     * limit; otherwise an {@link IllegalArgumentException} says which and why. Arguments are not logged.
     */
    public String render(String id, Map<String, String> values) {
        log.debug("Started");
        var template = template(id);
        for (var argument : template.arguments()) {
            var value = values.get(argument.name());
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("The '" + argument.name() + "' argument is required.");
            }
            if (value.length() > argument.maxLength()) {
                throw new IllegalArgumentException("The '" + argument.name() + "' argument is longer than "
                        + argument.maxLength() + " characters.");
            }
        }
        Matcher matcher = PLACEHOLDER.matcher(text(id));
        var result = new StringBuilder();
        while (matcher.find()) {
            var name = matcher.group(1);
            var value = values.get(name);
            if (value == null || template.arguments().stream().noneMatch(a -> a.name().equals(name))) {
                throw new IllegalStateException("Prompt " + id + " has an unknown placeholder: " + name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value.strip()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String load(String id) {
        log.debug("Started");
        var path = "/advisor/prompts/" + id + ".md";
        try (InputStream in = AdvisorPromptTemplates.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Prompt file missing from the classpath: " + path);
            }
            // Line endings normalized: a Windows checkout (the Jenkins agent) may turn LF into CRLF.
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read prompt " + path, e);
        }
    }
}
