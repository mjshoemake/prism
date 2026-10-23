package com.prism.api.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * One parity case: a metric grouped by one focus level, with drill-down filters. Identified by names (not IDs), so
 * the same case means the same thing in any database and keys survive re-seeding.
 * <p>
 * Key format (used in baseline and expected files, see {@code tools/parity/README.md}):
 * <pre>&lt;metric&gt; | &lt;group-by level&gt; | &lt;filters&gt;</pre>
 * where filters is {@code -} for none, or {@code Level=value} joined by {@code " & "}; a NULL value is {@code \N}.
 * Special characters in names and values are backslash-escaped ({@link #escape}).
 *
 * @param filters value null = the column is empty (IS NULL)
 */
record ParityCase(String metric, String level, List<Filter> filters) {

    static final String NULL_TEXT = "\\N";
    private static final String PART_SEPARATOR = " | ";
    private static final String FILTER_SEPARATOR = " & ";

    record Filter(String level, String value) {
    }

    ParityCase {
        filters = List.copyOf(filters);
    }

    ParityCase withFilter(String filterLevel, String value) {
        var more = new ArrayList<>(filters);
        more.add(new Filter(filterLevel, value));
        return new ParityCase(metric, level, more);
    }

    String key() {
        var text = new StringBuilder(escape(metric)).append(PART_SEPARATOR).append(escape(level)).append(PART_SEPARATOR);
        if (filters.isEmpty()) {
            text.append('-');
        }
        for (int i = 0; i < filters.size(); i++) {
            var filter = filters.get(i);
            text.append(i == 0 ? "" : FILTER_SEPARATOR).append(escape(filter.level())).append('=')
                .append(filter.value() == null ? NULL_TEXT : escape(filter.value()));
        }
        return text.toString();
    }

    /** Parses {@link #key()} output. */
    static ParityCase parseKey(String key) {
        var parts = splitUnescaped(key, PART_SEPARATOR);
        if (parts.size() != 3) {
            throw new IllegalArgumentException("A case key needs 3 parts (metric | level | filters): " + key);
        }
        var filters = new ArrayList<Filter>();
        if (!parts.get(2).equals("-")) {
            for (var filter : splitUnescaped(parts.get(2), FILTER_SEPARATOR)) {
                var pair = splitUnescaped(filter, "=");
                if (pair.size() != 2) {
                    throw new IllegalArgumentException("A filter needs the form Level=value: " + filter);
                }
                filters.add(new Filter(unescape(pair.get(0)), pair.get(1).equals(NULL_TEXT) ? null : unescape(pair.get(1))));
            }
        }
        return new ParityCase(unescape(parts.get(0)), unescape(parts.get(1)), filters);
    }

    /**
     * Escapes characters that have a meaning in the key and result formats: backslash, the separators
     * {@code | & =}, a leading {@code #} (header/comment lines) and line breaks.
     */
    static String escape(String text) {
        var out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\', '|', '&', '=', '#' -> out.append('\\').append(c);
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String unescape(String text) {
        var out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                out.append(next == 'n' ? '\n' : next == 'r' ? '\r' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Splits on {@code separator} where it isn't part of an escape sequence; parts stay escaped. */
    static List<String> splitUnescaped(String text, String separator) {
        var parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\\') {
                i++;
            } else if (text.startsWith(separator, i)) {
                parts.add(text.substring(start, i));
                i += separator.length() - 1;
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }
}
