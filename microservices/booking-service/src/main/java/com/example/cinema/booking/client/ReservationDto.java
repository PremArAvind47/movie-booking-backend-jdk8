package com.example.cinema.booking.client;

public record ReservationDto(String reservationKey, Long movieId, int seats) {
}
