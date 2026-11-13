package com.example.cinema.booking;

import com.example.cinema.booking.client.MovieClient;
import com.example.cinema.booking.client.MovieClientFallbackFactory;
import com.example.cinema.platform.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FallbackFactoryTest {

    private final MovieClientFallbackFactory factory = new MovieClientFallbackFactory();

    @Test
    void aBusinessAnswerPassesThroughUnchanged() {
        ApiException business = new ApiException(HttpStatus.CONFLICT, "NOT_ENOUGH_SEATS", "Only 1 left");
        MovieClient fallback = factory.create(new CompletionException(business));   // wrapped by the thread pool

        ApiException thrown = assertThrows(ApiException.class, () -> fallback.getMovie(1L, "r1"));

        assertSame(business, thrown);
    }

    @Test
    void anOutageBecomesServiceUnavailable() {
        MovieClient fallback = factory.create(new RuntimeException("Connection refused"));

        ApiException thrown = assertThrows(ApiException.class, () -> fallback.getMovie(1L, "r1"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatus());
        assertEquals("MOVIE_SERVICE_UNAVAILABLE", thrown.getCode());
    }
}
