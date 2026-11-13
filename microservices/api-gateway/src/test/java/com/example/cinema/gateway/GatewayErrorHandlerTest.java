package com.example.cinema.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayErrorHandlerTest {

    @Test
    void aRefusedConnectionIs503EvenWhenItIsWrapped() {
        Throwable wrapped = new RuntimeException("outer", new ConnectException("Connection refused"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, GatewayErrorHandler.statusFor(wrapped));
    }

    @Test
    void aConnectionClosedWithoutAnAnswerIs502() {
        class PrematureCloseException extends java.io.IOException { }   // same name as the reactor-netty class
        assertEquals(HttpStatus.BAD_GATEWAY, GatewayErrorHandler.statusFor(new PrematureCloseException()));
    }

    @Test
    void aTimeoutIs504() {
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, GatewayErrorHandler.statusFor(new TimeoutException("slow")));
    }

    @Test
    void aResponseStatusExceptionKeepsItsStatus() {
        assertEquals(HttpStatus.NOT_FOUND, GatewayErrorHandler.statusFor(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Test
    void anythingElseIs500() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, GatewayErrorHandler.statusFor(new IllegalStateException("bug")));
    }
}
