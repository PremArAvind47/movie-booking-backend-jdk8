package com.example.cinema.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdGlobalFilterTest {

    private String idSeenByTheService(String incomingHeader) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/v1/movies");
        if (incomingHeader != null) {
            builder.header("X-Request-Id", incomingHeader);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(builder.build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        GatewayFilterChain chain = e -> {
            seen.set(e);
            return Mono.empty();
        };
        new RequestIdGlobalFilter().filter(exchange, chain).block();
        return seen.get().getRequest().getHeaders().getFirst("X-Request-Id");
    }

    @Test
    void createsAnIdWhenThereIsNone() {
        String id = idSeenByTheService(null);
        assertEquals(8, id.length());
    }

    @Test
    void keepsASafeIdFromTheOutside() {
        assertEquals("abc-123", idSeenByTheService("abc-123"));
    }

    @Test
    void replacesAnUnsafeIdFromTheOutside() {
        String id = idSeenByTheService("bad value;\nx");
        assertNotEquals("bad value;\nx", id);
        assertTrue(id.matches("[A-Za-z0-9-]{8}"));
    }
}
