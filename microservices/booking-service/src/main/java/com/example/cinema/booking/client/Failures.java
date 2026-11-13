package com.example.cinema.booking.client;

import com.example.cinema.platform.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/** Decides what a failed call to another service means. */
final class Failures {

    private static final Logger log = LoggerFactory.getLogger(Failures.class);

    private Failures() {
    }

    /**
     * A real business answer (4xx) must reach the caller unchanged.
     * Anything else (timeout, connection refused, 5xx, open circuit) means "the service is not available".
     */
    static ApiException translate(Throwable cause, String serviceName, String code) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof ApiException api) {
                return api;
            }
        }
        log.warn("{} is not available: {}", serviceName, cause.toString());
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code,
                serviceName + " is not available right now. Please try again in a moment.");
    }
}
