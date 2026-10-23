package com.prism.api.metrics;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parity results as text, so they can be frozen in a file and compared exactly.
 * <pre>
 * # comment
 * ## &lt;header&gt;          one case: its key (baseline) or "description | key" (hand-checked expected file)
 * label|value          one line per chart row, in result order; \N = NULL label, empty value = NULL (no bar)
 * </pre>
 * Values are compared as text, so a different number of decimals counts as a difference.
 */
final class ParityResults {

    private static final String HEADER = "## ";

    private ParityResults() {
    }

    /** One chart row as text. */
    static String row(Object label, Object value) {
        return (label == null ? ParityCase.NULL_TEXT : ParityCase.escape(label.toString())) + "|" + text(value);
    }

    private static String text(Object value) {
        if (value == null) {
            return "";
        }
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
    }

    /** Reads a results file: header (without "## ") → rows, in file order. */
    static LinkedHashMap<String, List<String>> read(Path file) throws IOException {
        var cases = new LinkedHashMap<String, List<String>>();
        List<String> current = null;
        for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith(HEADER)) {
                current = new ArrayList<>();
                if (cases.put(line.substring(HEADER.length()), current) != null) {
                    throw new IllegalStateException(file + ": duplicate case " + line);
                }
            } else if (line.isBlank() || line.startsWith("#")) {
                continue;
            } else if (current == null) {
                throw new IllegalStateException(file + ": row before the first case: " + line);
            } else {
                current.add(line);
            }
        }
        return cases;
    }

    static void write(Path file, String comment, Map<String, List<String>> cases) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        var text = new StringBuilder();
        for (var line : comment.split("\n")) {
            text.append("# ").append(line).append('\n');
        }
        cases.forEach((key, rows) -> {
            text.append(HEADER).append(key).append('\n');
            rows.forEach(row -> text.append(row).append('\n'));
        });
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** Outcome of comparing actual results with expected ones. */
    record Comparison(int cases, int rows, List<String> differing, List<String> missing, List<String> unexpected,
                      Map<String, List<String>> expected, Map<String, List<String>> actual) {

        boolean identical() {
            return differing.isEmpty() && missing.isEmpty() && unexpected.isEmpty();
        }

        /** Counts, plus the first few differences in detail. */
        String report(String what) {
            return report(what, 10);
        }

        /** Counts, plus up to {@code examples} differences in detail. */
        String report(String what, int examples) {
            var text = new StringBuilder("%s: %d cases, %d rows, %d differing, %d missing, %d unexpected"
                    .formatted(what, cases, rows, differing.size(), missing.size(), unexpected.size()));
            for (var key : differing.subList(0, Math.min(examples, differing.size()))) {
                text.append("\n  differs: ").append(key)
                    .append("\n    expected ").append(expected.get(key))
                    .append("\n    actual   ").append(actual.get(key));
            }
            missing.stream().limit(5).forEach(key -> text.append("\n  missing (expected, not run): ").append(key));
            unexpected.stream().limit(5).forEach(key -> text.append("\n  unexpected (run, not expected): ").append(key));
            return text.toString();
        }
    }

    static Comparison compare(Map<String, List<String>> expected, Map<String, List<String>> actual) {
        var differing = new ArrayList<String>();
        var missing = new ArrayList<String>();
        int rows = 0;
        for (var entry : expected.entrySet()) {
            rows += entry.getValue().size();
            var got = actual.get(entry.getKey());
            if (got == null) {
                missing.add(entry.getKey());
            } else if (!Objects.equals(entry.getValue(), got)) {
                differing.add(entry.getKey());
            }
        }
        var unexpected = actual.keySet().stream().filter(key -> !expected.containsKey(key)).toList();
        return new Comparison(expected.size(), rows, differing, missing, unexpected, expected, actual);
    }
}
