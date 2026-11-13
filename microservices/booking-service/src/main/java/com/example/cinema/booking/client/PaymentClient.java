package com.example.cinema.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class,
        configuration = FeignClientConfig.class)
public interface PaymentClient {

    @PostMapping("/api/v1/payments")
    PaymentResult pay(@RequestBody PaymentRequest request, @RequestHeader("X-Request-Id") String requestId);
}
