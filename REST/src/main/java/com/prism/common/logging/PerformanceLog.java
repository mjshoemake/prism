package com.prism.common.logging;

import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Times an operation and logs the elapsed time to the {@code Performance} category.
 *
 * <pre>{@code
 * var rows = PerformanceLog.time("sprint-metrics query", () -> jdbcTemplate.queryForList(sql));
 * }</pre>
 */
public final class PerformanceLog {

    private static final Logger perf = LogCategory.PERFORMANCE.logger();

    private PerformanceLog() {
    }

    public static <T> T time(String operation, Supplier<T> work) {
        if (!perf.isInfoEnabled()) {
            return work.get();
        }
        long start = System.nanoTime();
        boolean succeeded = false;
        try {
            T result = work.get();
            succeeded = true;
            return result;
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            perf.info("{} {} in {} ms", operation, succeeded ? "completed" : "failed", elapsedMs);
        }
    }
}
