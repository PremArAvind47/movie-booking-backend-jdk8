package com.example.moviebooking.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// First safety check: runs in Java BEFORE the user's text goes to the AI.
// It is only the first line of defence. A clever user can rephrase, so the real protection
// is the permission check inside the tools (see MovieTools.getCustomerDetails).
@Component
public class InputGuard {

    private static final Logger log = LoggerFactory.getLogger(InputGuard.class);

    // typical "prompt injection" tricks: the user tries to override our rules
    private static final List<String> BLOCKED_PHRASES = List.of(
            "ignore previous instructions",
            "ignore all previous",
            "ignore your instructions",
            "disregard your instructions",
            "forget your instructions",
            "system prompt");

    private final int maxLength;

    public InputGuard(@Value("${guard.max-length}") int maxLength) {
        this.maxLength = maxLength;
    }

    public void check(String input) {
        if (input == null || input.isBlank()) {
            throw reject("The text is empty.");
        }
        if (input.length() > maxLength) {
            throw reject("The text is too long. The limit is " + maxLength + " characters.");
        }
        String text = input.toLowerCase().replaceAll("\\s+", " ");
        for (String phrase : BLOCKED_PHRASES) {
            if (text.contains(phrase)) {
                log.warn("Blocked a possible prompt injection: {}", input);
                throw reject("This text is not allowed.");
            }
        }
    }

    private ResponseStatusException reject(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
