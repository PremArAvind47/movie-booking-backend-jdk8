package com.example.cinema.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** paymentKey = the booking id. Sending the same key again never charges twice. */
public record PaymentRequest(@NotBlank String paymentKey, @NotNull @Positive Integer amount) {
}
