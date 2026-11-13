package com.example.cinema.platform;

import org.springframework.http.HttpStatus;

/**
 * A business error that we expect and want to show to the caller.
 * It carries an HTTP status and a short machine-readable code (for example NOT_ENOUGH_SEATS).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
