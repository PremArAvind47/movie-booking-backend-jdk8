package com.example.moviebooking.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

// Part 21: when the AI model (Ollama) cannot be reached, give a short, clear answer
// instead of a long crash message.
@RestControllerAdvice
public class AiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    // The model did not answer in time -> 504
    // Ollama is not running or not reachable -> 503
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleModelNotReachable(ResourceAccessException e) {
        if (isTimeout(e)) {
            log.warn("The AI model did not answer in time: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Map.of("error", "The AI model took too long to answer. Please try again."));
        }
        log.warn("The AI model is not reachable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "The AI model is not available. Is Ollama running?"));
    }

    // Look through the chain of causes: is one of them a timeout?
    // Different HTTP libraries use different timeout classes (SocketTimeoutException,
    // HttpTimeoutException, Netty's ReadTimeoutException...), but all have "TimeoutException" in the name.
    static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().endsWith("TimeoutException")) {
                return true;
            }
        }
        return false;
    }
}
