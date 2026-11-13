package com.example.cinema.booking;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In memory only (no database on purpose). Everything is lost when the service stops. */
@Repository
public class BookingRepository {

    private final ConcurrentHashMap<String, Booking> bookings = new ConcurrentHashMap<>();

    public void save(Booking booking) {
        bookings.put(booking.id(), booking);
    }

    public Optional<Booking> find(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> all() {
        return bookings.values().stream().sorted(Comparator.comparing(Booking::createdAt)).toList();
    }
}
