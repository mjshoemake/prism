package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Plain JUnit: keeps the curated catalog files valid. Every entry must load, link only to existing entries, and be a
 * metric Prism can actually calculate (same rules as {@code draft_metric}).
 */
class MetricCatalogTests {

    private static final Set<String> LEVELS = Set.of("foundation", "conditional", "outcome");

    private final MetricCatalog catalog = new MetricCatalog();
    private final MetricDraftService draft = new MetricDraftService();

    @Test
    void everyEntryLoads() {
        var ids = new HashSet<String>();
        for (var entry : catalog.all()) {
            assertTrue(ids.add(entry.id()), "duplicate id " + entry.id());
            assertTrue(LEVELS.contains(entry.level()), entry.id() + ": unknown level " + entry.level());
            assertFalse(entry.problems().isEmpty(), entry.id() + ": needs at least one problem");
            assertTrue(entry.body().contains("## Pitfalls"), entry.id() + ": needs a Pitfalls section");
            assertTrue(entry.uri().equals("prism://catalog/metrics/" + entry.id()));
        }
        assertTrue(ids.size() >= 15, "expected the curated entries, found " + ids);
    }

    @Test
    void relatedEntriesExist() {
        for (var entry : catalog.all()) {
            for (var related : entry.related()) {
                assertTrue(catalog.entry(related).isPresent(), entry.id() + ": related entry '" + related + "' doesn't exist");
            }
        }
    }

    @Test
    void everyEntryIsAValidPrismMetric() {
        for (var entry : catalog.all()) {
            var result = draft.draft(entry.name(), entry.type(), entry.numerator(), entry.denominator(),
                    entry.condition(), entry.name(), entry.unit(), entry.decimals());
            assertTrue(result.valid(), entry.id() + ": " + result.errors());
            assertTrue(result.warnings().isEmpty(), entry.id() + ": " + result.warnings());
        }
    }

    @Test
    void searchFindsTheRightEntryFirst() {
        assertEquals("change-failure-rate", catalog.search("too many failed deployments").get(0).id());
        assertEquals("say-do-ratio", catalog.search("our sprints keep missing commitments").get(0).id());
        assertEquals("mutation-score", catalog.search("are our tests any good? test quality").get(0).id());
        assertEquals("branches-merged-within-a-day", catalog.search("long-lived branches").get(0).id());
        assertEquals("wip-per-assignee", catalog.search("developers juggle too many tasks at once").get(0).id());
        assertEquals("commitment-strength", catalog.search("we always commit to more than we finish").get(0).id());
        assertEquals("scope-churn", catalog.search("leadership keeps interrupting with drop everything requests").get(0).id());
        assertEquals("feature-wip-per-team", catalog.search("too many features in flight").get(0).id());
    }

    @Test
    void searchLimitsAndEmptyInput() {
        assertTrue(catalog.search("deployments branches tests sprints commitments").size() <= MetricCatalog.MAX_RESULTS);
        assertEquals(List.of(), catalog.search(""));
        assertEquals(List.of(), catalog.search(null));
        assertEquals(List.of(), catalog.search("zzzz qqqq"));
    }

    @Test
    void indexListsEveryEntry() {
        var index = catalog.index();
        for (var entry : catalog.all()) {
            assertTrue(index.contains(entry.uri()), "index misses " + entry.id());
        }
    }

    @Test
    void parseRejectsBadFiles() {
        assertThrows(IllegalArgumentException.class, () -> MetricCatalog.parse("x", "no header"));
        assertThrows(IllegalArgumentException.class,
                () -> MetricCatalog.parse("x", "---\nid: y\nname: n\n---\nbody"));
        assertThrows(IllegalArgumentException.class,
                () -> MetricCatalog.parse("x", "---\nid: x\nname: n\n---\nbody")); // summary etc. missing
    }

    @Test
    void wordsIgnoreStopWordsAndPlurals() {
        assertEquals(Set.of("failed", "deployment"), MetricCatalog.words("The failed deployments"));
    }
}
