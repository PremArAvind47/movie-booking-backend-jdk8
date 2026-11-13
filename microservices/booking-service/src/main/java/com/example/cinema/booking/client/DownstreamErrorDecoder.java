package com.example.cinema.booking.client;

import com.example.cinema.platform.ApiError;
import com.example.cinema.platform.ApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 4xx answers are REAL business answers (movie not found, not enough seats). We turn them into an
 * ApiException with the same code and message, so the caller sees the real reason.
 * 5xx answers are failures of the other service: they stay a FeignException and count for the circuit breaker.
 */
public class DownstreamErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public DownstreamErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 400 && status < 500) {
            HttpStatus httpStatus = HttpStatus.valueOf(status);
            try {
                String body = response.body() == null ? "" : new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                ApiError error = objectMapper.readValue(body, ApiError.class);
                return new ApiException(httpStatus, error.code(), error.message());
            } catch (IOException | RuntimeException e) {
                return new ApiException(httpStatus, "DOWNSTREAM_ERROR", "The other service answered with status " + status);
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
