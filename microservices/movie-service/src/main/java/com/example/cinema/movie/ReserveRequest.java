package com.example.cinema.movie;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReserveRequest(@NotBlank String reservationKey, @NotNull Long movieId, @Min(1) int seats) {
}
