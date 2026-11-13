package com.example.cinema.gateway;

import com.example.cinema.platform.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

/** Errors made by the gateway itself (no route, service not found, timeout) use the same JSON format as the services. */
@Component
@Order(-2)   // before Spring Boot's default error handler
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        HttpStatusCode status = statusFor(error);
        String code = switch (status.value()) {
            case 404 -> "ROUTE_NOT_FOUND";
            case 502 -> "BAD_GATEWAY";
            case 503 -> "SERVICE_UNAVAILABLE";
            case 504 -> "GATEWAY_TIMEOUT";
            default -> "GATEWAY_ERROR";
        };
        String message = switch (status.value()) {
            case 404 -> "No route for " + exchange.getRequest().getURI().getPath();
            case 502 -> "The service closed the connection without an answer. Please try again.";
            case 503 -> "The service is not available right now. Please try again in a moment.";
            case 504 -> "The service took too long to answer.";
            default -> "Something went wrong at the gateway.";
        };
        String requestId = exchange.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.HEADER);
        if (requestId == null) {                       // for example "no route": the request id filter did not run
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        exchange.getResponse().getHeaders().set(RequestIdGlobalFilter.HEADER, requestId);
        log.warn("[{}] gateway error {} on {}: {}", requestId, status.value(),
                exchange.getRequest().getURI().getPath(), error.toString());

        ApiError body = new ApiError(Instant.now(), status.value(), code, message,
                exchange.getRequest().getURI().getPath(), requestId);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * What does the failure mean for the client?
     * A refused connection (the service just died, Eureka does not know yet) = 503.
     * The service closed the connection without an answer = 502. A timeout = 504. Spring's own ResponseStatusException keeps its status. Anything else = 500.
     */
    static HttpStatusCode statusFor(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ResponseStatusException rse) {
                return rse.getStatusCode();
            }
            String name = t.getClass().getSimpleName();
            if (name.equals("PrematureCloseException")) {      // the service died while we reused its connection
                return HttpStatus.BAD_GATEWAY;
            }
            if (name.endsWith("ConnectException")) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (name.endsWith("TimeoutException")) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
