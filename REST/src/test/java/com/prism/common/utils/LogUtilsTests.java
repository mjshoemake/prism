package com.prism.common.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

/** Plain unit tests; no Spring context needed. */
class LogUtilsTests {

    record Login(String email, String password) {
    }

    record Wrapper(String name, Login login, List<Integer> ids) {
    }

    public static class Node {
        private final String name;
        private Node next;

        public Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }
    }

    private static String dump(Object value) {
        return String.join("\n", LogUtils.dataToStrings(value));
    }

    @Test
    void recordsShowTheirComponents() {
        String out = dump(new Wrapper("w", new Login("a@x.com", "secret1"), List.of(1, 2)));

        assertTrue(out.contains("name = w"), out);
        assertTrue(out.contains("email = a@x.com"), out);
        assertTrue(out.contains("Item #0: 1"), out);
        assertTrue(out.contains("Item #1: 2"), out);
    }

    @Test
    void sensitiveTextValuesAreMasked() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("email", "a@x.com");
        row.put("password_hash", "$2a$12$abcdefghijklmnopqrstuv");
        row.put("is_temp_password", true);

        String out = dump(row) + "\n" + dump(new Login("a@x.com", "hunter2"));

        assertFalse(out.contains("hunter2"), out);
        assertFalse(out.contains("$2a$12$"), out);
        assertTrue(out.contains("password_hash = " + LogUtils.MASK), out);
        assertTrue(out.contains("password = " + LogUtils.MASK), out);
        assertTrue(out.contains("is_temp_password = true"), "flags are not masked:\n" + out);
    }

    @Test
    void valueTypesPrintTheirValue() {
        var id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("user_id", id);
        row.put("created", LocalDate.of(2026, 10, 20));
        row.put("scores", new int[] {7, 8});

        String out = dump(row);

        assertTrue(out.contains("user_id = " + id), out);
        assertTrue(out.contains("created = 2026-10-20"), out);
        assertTrue(out.contains("scores = int[2] {7, 8}"), out);
    }

    @Test
    void circularReferencesDoNotRecurseForever() {
        var a = new Node("a");
        var b = new Node("b");
        a.setNext(b);
        b.setNext(a);

        String out = dump(a);

        assertTrue(out.contains("circular reference"), out);
    }

    @Test
    void selfContainingListIsHandled() {
        List<Object> list = new ArrayList<>();
        list.add(list);

        assertTrue(dump(list).contains("circular reference"));
    }

    @Test
    void nullAndEmptyContainers() {
        assertArrayEquals(new String[] {"null"}, LogUtils.dataToStrings(null));
        assertEquals("Map is empty.", dump(Map.of()));
        assertEquals("List is empty.", dump(List.of()));
    }

    @Test
    void longToDurationFormatsAllFields() {
        assertEquals("00:00:00.000", LogUtils.longToDuration(0));
        assertEquals("01:02:03.004", LogUtils.longToDuration(3_723_004));
        assertEquals("12:00:00.000", LogUtils.longToDuration(12L * 3_600_000)); // was "null:12:00.000"
    }

    @Test
    void disabledLevelDoesNoWork() {
        // NOPLogger reports DEBUG/INFO disabled, so the object is never inspected.
        Object explodes = new Object() {
            @SuppressWarnings("unused")
            public String getBoom() {
                throw new AssertionError("should not be inspected");
            }
        };
        LogUtils.debug(NOPLogger.NOP_LOGGER, explodes);
        LogUtils.info(NOPLogger.NOP_LOGGER, explodes);
    }
}
