package com.example.cinema.booking;

import java.time.Instant;

public record Booking(String id, Long movieId, String movieTitle, int seats, int amount,
                      BookingStatus status, String reason, Instant createdAt) {

    public Booking withStatus(BookingStatus newStatus, String newReason) {
        return new Booking(id, movieId, movieTitle, seats, amount, newStatus, newReason, createdAt);
    }
}
