package com.example.cinema.payment;

public record PaymentResult(String paymentId, String paymentKey, int amount, PaymentStatus status, String message) {
}
