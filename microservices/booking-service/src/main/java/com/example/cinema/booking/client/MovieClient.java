package com.example.cinema.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Calls movie-service. The name "movie-service" is looked up in Eureka (no host or port in the code),
 * and the call is spread over all running copies (client-side load balancing).
 * The request id is passed as a normal parameter on purpose: the circuit breaker runs the call
 * in another thread, where the log context (MDC) is not available.
 */
@FeignClient(name = "movie-service", fallbackFactory = MovieClientFallbackFactory.class,
        configuration = FeignClientConfig.class)
public interface MovieClient {

    @GetMapping("/api/v1/movies/{id}")
    MovieDto getMovie(@PathVariable("id") Long id, @RequestHeader("X-Request-Id") String requestId);

    @PostMapping("/api/v1/reservations")
    ReservationDto reserve(@RequestBody ReserveRequest request, @RequestHeader("X-Request-Id") String requestId);

    @DeleteMapping("/api/v1/reservations/{key}")
    void release(@PathVariable("key") String key, @RequestHeader("X-Request-Id") String requestId);
}
