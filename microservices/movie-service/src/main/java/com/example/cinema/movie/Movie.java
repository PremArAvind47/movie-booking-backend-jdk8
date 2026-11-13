package com.example.cinema.movie;

/** A movie in the catalog. Price is in rupees per seat. */
public record Movie(Long id, String title, int price) {
}
