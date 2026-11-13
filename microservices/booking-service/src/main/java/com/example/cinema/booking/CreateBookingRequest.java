package com.example.cinema.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(@NotNull Long movieId, @Min(1) int seats) {
}
