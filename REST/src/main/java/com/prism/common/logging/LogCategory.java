package com.prism.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging categories. Each category is a top-level logger name whose verbosity is set independently in
 * {@code application.properties} (e.g. {@code logging.level.SQL=DEBUG}); where the output goes is set in
 * {@code logback-spring.xml}.
 * <p>
 * Class-scoped loggers are children of their category ({@code Service.UsersService}), so a single class
 * can also be tuned on its own ({@code logging.level.Service.UsersService=DEBUG}).
 *
 * <pre>{@code
 * private static final Logger log   = LogCategory.SERVICE.logger(UsersService.class);
 * private static final Logger audit = LogCategory.AUDIT.logger();
 * }</pre>
 */
public enum LogCategory {

    /** Application lifecycle (startup/shutdown). */
    APPLICATION("Application"),
    /** REST controllers: request handling. */
    CONTROLLER("Controller"),
    /** Business logic in services. */
    SERVICE("Service"),
    /** Generated SQL statements (verbose; DEBUG). */
    SQL("SQL"),
    /** Security-relevant events (logins, user and password changes). Also written to audit.log. */
    AUDIT("Audit"),
    /** Timings of expensive operations. Also written to perf.log. */
    PERFORMANCE("Performance");

    private final String loggerName;

    LogCategory(String loggerName) {
        this.loggerName = loggerName;
    }

    public String loggerName() {
        return loggerName;
    }

    /** The category logger itself, e.g. {@code Audit}. */
    public Logger logger() {
        return LoggerFactory.getLogger(loggerName);
    }

    /** A logger for one class within the category, e.g. {@code Service.UsersService}. */
    public Logger logger(Class<?> type) {
        return LoggerFactory.getLogger(loggerName + "." + type.getSimpleName());
    }
}
