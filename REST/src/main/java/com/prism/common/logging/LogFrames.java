package com.prism.common.logging;

import java.util.List;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Hides logging helper classes from Logback's caller data, so {@code %method} in the log pattern shows
 * the method that called the helper (e.g. {@code UsersService.createUser}) rather than the helper's own
 * method (e.g. {@code LogUtils.debug}).
 * <p>
 * Uses Logback's {@code LoggerContext.getFrameworkPackages()} via reflection so there is no compile-time
 * dependency on Logback; with any other SLF4J backend this is a no-op.
 */
public final class LogFrames {

    private LogFrames() {
    }

    public static synchronized void skip(Class<?> helper) {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        try {
            if (factory.getClass().getMethod("getFrameworkPackages").invoke(factory) instanceof List<?> packages) {
                @SuppressWarnings("unchecked")
                var names = (List<String>) packages;
                if (!names.contains(helper.getName())) {
                    names.add(helper.getName());
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Not Logback (or an unmodifiable list): %method will show the helper's method instead.
        }
    }
}
