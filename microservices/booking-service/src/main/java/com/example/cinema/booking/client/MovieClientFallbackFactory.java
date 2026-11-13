package com.example.cinema.booking.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Runs when the call to movie-service fails or the circuit is open. It never invents data: it throws a clear error. */
@Component
public class MovieClientFallbackFactory implements FallbackFactory<MovieClient> {

    private static final String CODE = "MOVIE_SERVICE_UNAVAILABLE";

    @Override
    public MovieClient create(Throwable cause) {
        return new MovieClient() {
            @Override
            public MovieDto getMovie(Long id, String requestId) {
                throw Failures.translate(cause, "movie-service", CODE);
            }

            @Override
            public ReservationDto reserve(ReserveRequest request, String requestId) {
                throw Failures.translate(cause, "movie-service", CODE);
            }

            @Override
            public void release(String key, String requestId) {
                throw Failures.translate(cause, "movie-service", CODE);
            }
        };
    }
}
