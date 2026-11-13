package com.example.cinema.platform;

import java.time.Instant;

/** The ONE error format that every service returns. */
public record ApiError(Instant timestamp, int status, String code, String message, String path, String requestId) {
}
