package com.prism.api.metrics;

/**
 * Metric data can't be fetched right now for a reason on the server's side, not the request's: the read-only metric
 * login isn't set up or can't connect, it isn't safe to use, or the query ran past its time limit. Answered with 503
 * and the message, which says what to do.
 */
public class MetricDataUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MetricDataUnavailableException(String message) {
        super(message);
    }

    public MetricDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
