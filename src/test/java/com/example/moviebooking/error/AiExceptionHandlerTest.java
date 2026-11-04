package com.example.moviebooking.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Fast tests for the error answers. They do not need Ollama.
class AiExceptionHandlerTest {

    private final AiExceptionHandler handler = new AiExceptionHandler();

    @Test
    void connectionRefusedGives503() {
        var response = handler.handleModelNotReachable(
                new ResourceAccessException("I/O error", new ConnectException("Connection refused")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("Ollama"));
    }

    @Test
    void streamingConnectionRefusedGives503() {
        var request = new org.springframework.web.reactive.function.client.WebClientRequestException(
                new ConnectException("Connection refused"), org.springframework.http.HttpMethod.POST,
                java.net.URI.create("http://localhost:11434/api/chat"), org.springframework.http.HttpHeaders.EMPTY);
        var response = handler.handleModelNotReachable(request);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void socketTimeoutGives504() {
        var response = handler.handleModelNotReachable(
                new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void httpTimeoutGives504() {
        var response = handler.handleModelNotReachable(
                new ResourceAccessException("I/O error", new HttpTimeoutException("request timed out")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void nettyStyleReadTimeoutGives504() {
        // the real one is io.netty.handler.timeout.ReadTimeoutException; same name ending
        class ReadTimeoutException extends IOException { }
        var response = handler.handleModelNotReachable(
                new ResourceAccessException("I/O error", new IOException(new ReadTimeoutException())));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void timeoutHiddenDeeperInTheCausesIsStillFound() {
        var deep = new IOException("wrapper", new SocketTimeoutException("Read timed out"));
        var response = handler.handleModelNotReachable(new ResourceAccessException("I/O error", deep));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }
}
