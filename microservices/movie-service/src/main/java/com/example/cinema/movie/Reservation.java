package com.example.cinema.movie;

/** Seats held for one booking. The key is the booking id (it makes the call safe to repeat). */
public record Reservation(String reservationKey, Long movieId, int seats) {
}
