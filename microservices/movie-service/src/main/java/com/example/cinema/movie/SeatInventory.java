package com.example.cinema.movie;

import com.example.cinema.platform.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hardcoded catalog and seat counts, kept in memory (no database on purpose).
 * Methods are synchronized so two requests cannot take the same last seat.
 */
@Service
public class SeatInventory {

    private final List<Movie> movies = List.of(
            new Movie(1L, "Avengers", 250),
            new Movie(2L, "Batman", 200),
            new Movie(3L, "Dune", 300));

    private final Map<Long, Integer> freeSeats = new HashMap<>(Map.of(1L, 42, 2L, 7, 3L, 120));
    private final Map<String, Reservation> reservations = new HashMap<>();

    public List<MovieView> list() {
        return movies.stream().map(this::view).toList();
    }

    public synchronized MovieView get(Long movieId) {
        return view(find(movieId));
    }

    /** Idempotent: the same key twice returns the same reservation and takes the seats only once. */
    public synchronized Reservation reserve(String key, Long movieId, int seats) {
        Reservation existing = reservations.get(key);
        if (existing != null) {
            return existing;
        }
        Movie movie = find(movieId);
        int free = freeSeats.get(movieId);
        if (free < seats) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_ENOUGH_SEATS",
                    "Only " + free + " seat(s) left for " + movie.title());
        }
        freeSeats.put(movieId, free - seats);
        Reservation reservation = new Reservation(key, movieId, seats);
        reservations.put(key, reservation);
        return reservation;
    }

    /** Idempotent: releasing an unknown or already released key does nothing. */
    public synchronized void release(String key) {
        Reservation reservation = reservations.remove(key);
        if (reservation != null) {
            freeSeats.merge(reservation.movieId(), reservation.seats(), Integer::sum);
        }
    }

    private Movie find(Long movieId) {
        return movies.stream()
                .filter(m -> m.id().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND",
                        "No movie with id " + movieId));
    }

    private MovieView view(Movie movie) {
        return new MovieView(movie.id(), movie.title(), movie.price(), freeSeats.get(movie.id()));
    }
}
