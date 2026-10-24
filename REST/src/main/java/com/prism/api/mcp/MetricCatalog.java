package com.prism.api.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.prism.common.logging.LogCategory;

/**
 * The Advisor's metric catalog: owner-curated Markdown files with a YAML header under
 * {@code src/main/resources/advisor/catalog/}, versioned in git (docs/MCP.md §4.1). Loaded once, on first use.
 * A file that can't be read is skipped with a warning; {@code MetricCatalogTests} keeps the curated files valid.
 */
@Component
public class MetricCatalog {

    private static final Logger log = LogCategory.SERVICE.logger(MetricCatalog.class);

    static final String LOCATION = "classpath*:advisor/catalog/*.md";
    private static final Pattern HEADER = Pattern.compile("\\A---\\R(.*?)\\R---\\R?(.*)\\z", Pattern.DOTALL);
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    /** Words too common to help a search. */
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "for", "how", "i", "in", "is", "it",
            "my", "of", "on", "or", "our", "the", "to", "too", "we", "what", "with");
    static final int MAX_RESULTS = 5;

    /** A search hit: enough to choose an entry, then read it by {@code uri}. */
    public record Hit(String id, String name, String summary, String level, String type, String uri, int score) {
    }

    private volatile Map<String, CatalogEntry> entries;

    /** All entries, by name. */
    public Collection<CatalogEntry> all() {
        log.debug("Started");
        return entries().values();
    }

    public Optional<CatalogEntry> entry(String id) {
        log.debug("Started");
        return Optional.ofNullable(id == null ? null : entries().get(id));
    }

    /**
     * Entries matching a described problem, best first (at most {@value #MAX_RESULTS}). Deterministic word matching:
     * a word found in an entry's problems or name counts 3, in its tags or summary 2, in its body 1.
     */
    public List<Hit> search(String problem) {
        log.debug("Started");
        var words = words(problem);
        if (words.isEmpty()) {
            return List.of();
        }
        var hits = new ArrayList<Hit>();
        for (var entry : all()) {
            var strong = words(entry.name() + " " + String.join(" ", entry.problems()));
            var medium = words(entry.summary() + " " + String.join(" ", entry.tags()));
            var weak = words(entry.body());
            int score = 0;
            for (var word : words) {
                score += strong.contains(word) ? 3 : medium.contains(word) ? 2 : weak.contains(word) ? 1 : 0;
            }
            if (score > 0) {
                hits.add(new Hit(entry.id(), entry.name(), entry.summary(), entry.level(), entry.type(), entry.uri(), score));
            }
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed().thenComparing(Hit::name));
        return List.copyOf(hits.subList(0, Math.min(MAX_RESULTS, hits.size())));
    }

    /** A Markdown index of the catalog, served as {@code prism://catalog/metrics}. */
    public String index() {
        log.debug("Started");
        var text = new StringBuilder("""
                # Prism metric catalog

                Curated metrics: what each one tells you, how Prism calculates it, the data it needs and its pitfalls.
                Read an entry at its URI. Level: foundation (helps almost any team), conditional (helps only on that
                foundation), outcome (what leaders ask for). See prism://guide/improvement-paths.

                | Metric | Level | Type | Summary | URI |
                |---|---|---|---|---|
                """);
        for (var entry : all()) {
            text.append("| ").append(entry.name()).append(" | ").append(entry.level()).append(" | ").append(entry.type())
                .append(" | ").append(entry.summary()).append(" | ").append(entry.uri()).append(" |\n");
        }
        return text.toString();
    }

    private Map<String, CatalogEntry> entries() {
        var loaded = entries;
        if (loaded == null) {
            synchronized (this) {
                if (entries == null) {
                    entries = load();
                }
                loaded = entries;
            }
        }
        return loaded;
    }

    private static Map<String, CatalogEntry> load() {
        log.debug("Started");
        Resource[] files;
        try {
            files = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the metric catalog (" + LOCATION + ")", e);
        }
        var byName = new ArrayList<CatalogEntry>();
        for (var file : files) {
            try (InputStream in = file.getInputStream()) {
                var id = file.getFilename() == null ? "" : file.getFilename().replaceFirst("\\.md$", "");
                byName.add(parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException | RuntimeException e) {
                log.warn("Catalog entry {} skipped: {}", file.getFilename(), e.getMessage());
            }
        }
        byName.sort(Comparator.comparing(CatalogEntry::name));
        var map = new LinkedHashMap<String, CatalogEntry>();
        byName.forEach(entry -> map.put(entry.id(), entry));
        log.info("Metric catalog loaded: {} entries", map.size());
        return map;
    }

    /** Parses one catalog file. {@code id} is the file name without {@code .md}; the header's id must match it. */
    static CatalogEntry parse(String id, String text) {
        log.debug("Started");
        var matcher = HEADER.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("missing the YAML header between --- lines");
        }
        Map<String, Object> header = new Yaml().load(matcher.group(1));
        if (header == null) {
            throw new IllegalArgumentException("empty YAML header");
        }
        var headerId = string(header, "id");
        if (!id.equals(headerId)) {
            throw new IllegalArgumentException("id '" + headerId + "' doesn't match the file name '" + id + "'");
        }
        var decimals = header.get("decimals");
        return new CatalogEntry(id, required(header, "name"), required(header, "summary"), required(header, "level"),
                list(header, "problems"), list(header, "tags"), required(header, "type"), required(header, "numerator"),
                string(header, "denominator"), string(header, "condition"), string(header, "unit"),
                decimals instanceof Number number ? number.intValue() : null, string(header, "grain"),
                list(header, "related"), list(header, "prerequisites"), matcher.group(2).strip(), text);
    }

    private static String required(Map<String, Object> header, String key) {
        var value = string(header, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("'" + key + "' is required");
        }
        return value;
    }

    private static String string(Map<String, Object> header, String key) {
        var value = header.get(key);
        return value == null ? null : value.toString();
    }

    private static List<String> list(Map<String, Object> header, String key) {
        return header.get(key) instanceof List<?> values ? values.stream().map(String::valueOf).toList() : List.of();
    }

    /** Lower-case words, without stop words; a trailing "s" is dropped so "deployments" matches "deployment". */
    static Set<String> words(String text) {
        if (text == null) {
            return Set.of();
        }
        var matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        var words = new ArrayList<String>();
        while (matcher.find()) {
            var word = matcher.group();
            if (!STOP_WORDS.contains(word)) {
                words.add(word.length() > 3 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word);
            }
        }
        return words.stream().collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
