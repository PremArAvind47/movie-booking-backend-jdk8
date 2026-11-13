package com.example.cinema.booking.client;

public record ReserveRequest(String reservationKey, Long movieId, int seats) {
}
