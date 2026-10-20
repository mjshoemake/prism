package com.prism.common.utils;

import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.prism.common.exceptions.CoreException;
import com.prism.common.logging.LogCategory;
import com.prism.common.logging.LogFrames;

/**
 * Turns any object into readable log lines describing its state: JavaBeans (getters), records,
 * {@link Map}s, {@link Collection}s, arrays and XML DOM nodes, nested to any depth.
 *
 * <pre>{@code
 * LogUtils.debug(log, request);          // one DEBUG line per property, at the caller's logger
 * String[] lines = LogUtils.dataToStrings(row);
 * }</pre>
 *
 * Safety features:
 * <ul>
 *   <li>Text values whose name looks sensitive (password, secret, token, hash, credential, api key…)
 *       are printed as {@value #MASK}.</li>
 *   <li>Circular references are reported instead of recursing forever; nesting stops at
 *       {@value #MAX_DEPTH} levels.</li>
 *   <li>{@code debug}/{@code info} do no work when that level is disabled.</li>
 * </ul>
 */
public final class LogUtils {

    private static final Logger log = LogCategory.CORE.logger(LogUtils.class);

    static final String MASK = "********";
    static final int MAX_DEPTH = 12;
    private static final int MAX_ARRAY_ITEMS = 64;
    private static final String DEFAULT_PREFIX = "   ";

    /** Property / map-key names whose text values are masked. */
    private static final Pattern SENSITIVE_NAME = Pattern.compile(
            "(?i).*(password|passwd|pwd|secret|token|hash|credential|api[_-]?key|private[_-]?key).*");

    static {
        // Make %method in the log pattern show LogUtils' caller, not LogUtils.debug/info.
        LogFrames.skip(LogUtils.class);
    }

    private LogUtils() {
    }

    // ---- Logging entry points --------------------------------------------------------------------

    /** Logs the object's contents at DEBUG, one line per value. */
    public static void debug(Logger logger, Object obj) {
        debug(logger, obj, DEFAULT_PREFIX, true);
    }

    /** Logs the object's contents at DEBUG with a custom line prefix; {@code showTypes} adds class names. */
    public static void debug(Logger logger, Object obj, String prefix, boolean showTypes) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        for (String line : dataToStrings(obj, showTypes)) {
            logger.debug("{}{}", prefix, line);
        }
    }

    /** Logs the object's contents at INFO, one line per value. */
    public static void info(Logger logger, Object obj) {
        info(logger, obj, DEFAULT_PREFIX, true);
    }

    /** Logs the object's contents at INFO with a custom line prefix; {@code showTypes} adds class names. */
    public static void info(Logger logger, Object obj, String prefix, boolean showTypes) {
        if (!logger.isInfoEnabled()) {
            return;
        }
        for (String line : dataToStrings(obj, showTypes)) {
            logger.info("{}{}", prefix, line);
        }
    }

    /**
     * Writes the object's contents at INFO to the {@code Core} category (formerly System.out, which
     * bypassed the logging configuration).
     */
    public static void println(Object obj, String prefix, boolean showTypes) {
        info(log, obj, prefix, showTypes);
    }

    // ---- Conversion ------------------------------------------------------------------------------

    /** Describes the object as lines of text, including class names of nested objects. */
    public static String[] dataToStrings(Object bean) {
        return dataToStrings(bean, true);
    }

    /**
     * Describes the object as lines of text.
     *
     * @param showTypes include class names of nested objects
     * @throws CoreException if the object cannot be inspected
     */
    public static String[] dataToStrings(Object bean, boolean showTypes) {
        if (bean == null) {
            return new String[] {"null"};
        }
        try {
            var dumper = new Dumper(showTypes);
            dumper.dumpRoot(bean);
            return dumper.lines.toArray(String[]::new);
        } catch (RuntimeException e) {
            var ex = new CoreException("Error loading bean (" + bean.getClass().getName() + ").", e);
            log.warn("Error describing object for logging.", ex);
            throw ex;
        }
    }

    // ---- Misc helpers ----------------------------------------------------------------------------

    public static String getStackTraceAsString(Throwable ex) {
        var writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /** Indentation for the given nesting level (3 spaces per level). */
    public static String indent(int level) {
        return "   ".repeat(Math.max(0, level));
    }

    /** Formats milliseconds as a duration, {@code HH:MM:SS.mmm} (hours may exceed 99). */
    public static String longToDuration(long millis) {
        long ms = millis % 1000;
        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        return "%02d:%02d:%02d.%03d".formatted(hours, minutes, seconds, ms);
    }

    // ---- Implementation --------------------------------------------------------------------------

    private static final class Dumper {

        final List<String> lines = new ArrayList<>();
        private final boolean showTypes;
        /** Objects on the current path, for cycle detection (identity, not equals). */
        private final Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());

        Dumper(boolean showTypes) {
            this.showTypes = showTypes;
        }

        void dumpRoot(Object bean) {
            if (isLeaf(bean)) {
                lines.add(leafText(bean));
            } else {
                nested(bean, 0);
            }
        }

        /** Adds "name = value" (or "Item #n: value"); complex values get a type line and nested lines. */
        private void member(String name, String separator, Object value, int level, boolean maskable) {
            String prefix = indent(level) + name + separator;
            if (value == null) {
                lines.add(prefix + "null");
            } else if (maskable && isSensitive(name, value)) {
                lines.add(prefix + MASK);
            } else if (name.endsWith("Class") || name.endsWith("class")) {
                lines.add(prefix + "SKIPPING");
            } else if (isLeaf(value)) {
                lines.add(prefix + leafText(value));
            } else {
                lines.add(prefix + typeLabel(value));
                nested(value, level + 1);
            }
        }

        private void nested(Object value, int level) {
            if (level > MAX_DEPTH) {
                lines.add(indent(level) + "(max depth " + MAX_DEPTH + " reached)");
                return;
            }
            if (!path.add(value)) {
                lines.add(indent(level) + "(circular reference to " + value.getClass().getName() + ")");
                return;
            }
            try {
                switch (value) {
                    case Map<?, ?> map -> map(map, level);
                    case Collection<?> collection -> items(collection.toArray(), level);
                    case Object[] array -> items(array, level);
                    case Document document -> document(document, level);
                    case Node node -> node(node, level);
                    default -> {
                        if (value.getClass().isRecord()) {
                            record(value, level);
                        } else {
                            bean(value, level);
                        }
                    }
                }
            } finally {
                path.remove(value);
            }
        }

        private void map(Map<?, ?> map, int level) {
            if (map.isEmpty()) {
                lines.add(indent(level) + "Map is empty.");
                return;
            }
            map.forEach((key, val) -> member(String.valueOf(key), " = ", val, level, true));
        }

        private void items(Object[] items, int level) {
            if (items.length == 0) {
                lines.add(indent(level) + "List is empty.");
                return;
            }
            for (int i = 0; i < items.length; i++) {
                member("Item #" + i, ": ", items[i], level, false);
            }
        }

        private void record(Object record, int level) {
            for (RecordComponent component : record.getClass().getRecordComponents()) {
                String name = component.getName();
                try {
                    Method accessor = component.getAccessor();
                    accessor.trySetAccessible();
                    member(name, " = ", accessor.invoke(record), level, true);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    lines.add(indent(level) + name + " = ERROR: " + rootMessage(e));
                }
            }
        }

        private void bean(Object bean, int level) {
            int before = lines.size();
            for (PropertyDescriptor property : BeanUtils.getPropertyDescriptors(bean.getClass())) {
                String name = property.getName();
                if (name.equalsIgnoreCase("class") || name.equalsIgnoreCase("parent")
                        || name.equalsIgnoreCase("declaringClass")) {
                    continue;
                }
                Method getter = property.getReadMethod();
                if (getter == null) {
                    lines.add(indent(level) + name + " = NO GET METHOD FOUND.");
                    continue;
                }
                try {
                    getter.trySetAccessible();
                    member(name, " = ", getter.invoke(bean), level, true);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    lines.add(indent(level) + name + " = ERROR: " + rootMessage(e));
                }
            }
            if (lines.size() == before) {
                // No readable properties (e.g. a class without getters): fall back to toString().
                lines.add(indent(level) + bean);
            }
        }

        private void document(Document doc, int level) {
            lines.add(indent(level) + "XML Document:");
            attributes(doc.getAttributes(), level + 2);
            children(doc.getFirstChild(), level + 1);
        }

        private void node(Node node, int level) {
            String name = node.getNodeName();
            String value = node.getNodeValue();
            Node firstChild = node.getFirstChild();
            if (firstChild != null && "#text".equals(firstChild.getNodeName())) {
                value = firstChild.getNodeValue();
            }
            lines.add(indent(level) + (value == null || value.isBlank() ? name : name + " = " + value));
            attributes(node.getAttributes(), level + 1);
            children(firstChild, level + 1);
        }

        private void attributes(NamedNodeMap attributes, int level) {
            if (attributes == null) {
                return;
            }
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String value = attr.getNodeValue();
                lines.add(indent(level) + "Attr: " + attr.getNodeName() + (value == null ? "" : " = " + value));
            }
        }

        private void children(Node child, int level) {
            for (Node n = child; n != null; n = n.getNextSibling()) {
                if (!"#text".equals(n.getNodeName())) {
                    nested(n, level);
                }
            }
        }

        private String typeLabel(Object value) {
            if (showTypes) {
                return value.getClass().getName();
            }
            return switch (value) {
                case Map<?, ?> m -> "Map";
                case Collection<?> c -> "List";
                case Object[] a -> "Array";
                default -> value.getClass().getSimpleName();
            };
        }
    }

    // ---- Value classification --------------------------------------------------------------------

    /** Values printed inline: strings, numbers, booleans, dates/times, enums, UUIDs, primitive arrays… */
    private static boolean isLeaf(Object value) {
        Class<?> type = value.getClass();
        return BeanUtils.isSimpleValueType(type)
                || (type.isArray() && type.getComponentType().isPrimitive());
    }

    private static String leafText(Object value) {
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            var text = new StringBuilder(value.getClass().getComponentType().getName())
                    .append('[').append(length).append("] {");
            for (int i = 0; i < Math.min(length, MAX_ARRAY_ITEMS); i++) {
                text.append(i == 0 ? "" : ", ").append(Array.get(value, i));
            }
            return text.append(length > MAX_ARRAY_ITEMS ? ", …}" : "}").toString();
        }
        return String.valueOf(value);
    }

    /** Text-like values under a sensitive-sounding name are masked; flags, counts and dates are not. */
    private static boolean isSensitive(String name, Object value) {
        return (value instanceof CharSequence || value instanceof char[] || value instanceof byte[])
                && SENSITIVE_NAME.matcher(name).matches();
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
    }
}
