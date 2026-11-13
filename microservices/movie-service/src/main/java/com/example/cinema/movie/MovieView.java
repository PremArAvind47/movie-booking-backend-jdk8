package com.example.cinema.movie;

/** What we show to the caller: the movie plus how many seats are free right now. */
public record MovieView(Long id, String title, int price, int freeSeats) {
}
