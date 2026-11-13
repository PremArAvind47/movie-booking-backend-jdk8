package com.example.cinema.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** Comes from the Config Server (config-repo/booking-service.yml). Refreshable without a restart. */
@Component
@RefreshScope
public class BookingRules {

    private final int maxSeatsPerBooking;

    public BookingRules(@Value("${booking.max-seats-per-booking:6}") int maxSeatsPerBooking) {
        this.maxSeatsPerBooking = maxSeatsPerBooking;
    }

    public int maxSeatsPerBooking() {
        return maxSeatsPerBooking;
    }
}
