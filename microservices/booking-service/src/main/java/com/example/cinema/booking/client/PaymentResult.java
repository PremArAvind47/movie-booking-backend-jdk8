package com.example.cinema.booking.client;

public record PaymentResult(String paymentId, String paymentKey, int amount, String status, String message) {

    public boolean approved() {
        return "APPROVED".equals(status);
    }
}
