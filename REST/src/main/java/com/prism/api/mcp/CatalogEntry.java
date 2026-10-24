package com.prism.api.mcp;

import java.util.List;

/**
 * One metric in the Advisor's catalog (docs/MCP.md §4.1): the YAML header of {@code advisor/catalog/<id>.md} plus the
 * Markdown body.
 *
 * @param level       foundation, conditional or outcome (prism://guide/improvement-paths)
 * @param type        Prism metric type (percentage, ratio, average, sum)
 * @param numerator   numerator, or the value for average/sum, using an example fact alias
 * @param denominator percentage/ratio only; null otherwise
 * @param condition   optional row condition; null if none
 * @param decimals    null = Prism's default
 * @param related     IDs of related catalog entries
 * @param text        the whole file (header and body), as served to assistants
 */
public record CatalogEntry(String id, String name, String summary, String level, List<String> problems,
                           List<String> tags, String type, String numerator, String denominator, String condition,
                           String unit, Integer decimals, String grain, List<String> related,
                           List<String> prerequisites, String body, String text) {

    public static final String URI_PREFIX = "prism://catalog/metrics/";

    public String uri() {
        return URI_PREFIX + id;
    }
}
