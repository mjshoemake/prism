package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Plain JUnit: every listed guide has its Markdown file on the classpath; unknown IDs are refused. */
class AdvisorGuidesTests {

    private final AdvisorGuides guides = new AdvisorGuides();

    @Test
    void everyGuideLoads() {
        for (var guide : AdvisorGuides.GUIDES) {
            var text = guides.text(guide.id());
            assertTrue(text.startsWith("# "), guide.id() + " should start with a Markdown title");
            assertTrue(guide.uri().startsWith("prism://guide/"));
        }
    }

    @Test
    void metricTypesGuideExplainsRatioOfSums() {
        var text = guides.text("metric-types");
        assertTrue(text.contains("38 %") && text.contains("57.5 %"));
        assertEquals("prism://guide/metric-types", guides.guide("metric-types").uri());
    }

    @Test
    void unknownGuideIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> guides.text("../application.properties"));
    }
}
