package com.example.cinema.booking;

public enum BookingStatus {
    CONFIRMED,
    PAYMENT_DECLINED,
    /** The booking failed (for example the payment service was down) and the seats were released. */
    FAILED,
    /** The booking failed AND releasing the seats also failed. A person or a repair job must fix it. */
    NEEDS_ATTENTION
}
