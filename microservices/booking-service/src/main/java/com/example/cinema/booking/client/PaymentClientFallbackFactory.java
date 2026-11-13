package com.example.cinema.booking.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {

    @Override
    public PaymentClient create(Throwable cause) {
        return (request, requestId) -> {
            throw Failures.translate(cause, "payment-service", "PAYMENT_SERVICE_UNAVAILABLE");
        };
    }
}
