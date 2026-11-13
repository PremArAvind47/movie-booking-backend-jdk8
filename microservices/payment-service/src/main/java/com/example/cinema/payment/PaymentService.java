package com.example.cinema.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pretend payment provider: approves small amounts, declines big ones. No real money, no database. */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRules rules;
    private final Map<String, PaymentResult> processed = new HashMap<>();

    public PaymentService(PaymentRules rules) {
        this.rules = rules;
    }

    /** Idempotent: the same paymentKey returns the first result and does not charge again. */
    public synchronized PaymentResult pay(PaymentRequest request) {
        PaymentResult existing = processed.get(request.paymentKey());
        if (existing != null) {
            log.info("Payment {} was already processed, returning the first result", request.paymentKey());
            return existing;
        }
        PaymentResult result;
        if (request.amount() > rules.declineAbove()) {
            result = new PaymentResult(UUID.randomUUID().toString(), request.paymentKey(), request.amount(),
                    PaymentStatus.DECLINED, "Amount is above the limit of " + rules.declineAbove());
        } else {
            result = new PaymentResult(UUID.randomUUID().toString(), request.paymentKey(), request.amount(),
                    PaymentStatus.APPROVED, "Approved");
        }
        processed.put(request.paymentKey(), result);
        log.info("Payment {} -> {}", request.paymentKey(), result.status());
        return result;
    }
}
