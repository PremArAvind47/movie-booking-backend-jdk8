package com.example.cinema.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService(new PaymentRules(1000));

    @Test
    void smallAmountIsApproved() {
        assertEquals(PaymentStatus.APPROVED, service.pay(new PaymentRequest("b1", 500)).status());
    }

    @Test
    void amountAtTheLimitIsApproved() {
        assertEquals(PaymentStatus.APPROVED, service.pay(new PaymentRequest("b1", 1000)).status());
    }

    @Test
    void amountAboveTheLimitIsDeclined() {
        assertEquals(PaymentStatus.DECLINED, service.pay(new PaymentRequest("b1", 1001)).status());
    }

    @Test
    void sameKeyTwiceReturnsTheFirstResult() {
        PaymentResult first = service.pay(new PaymentRequest("b1", 500));
        PaymentResult second = service.pay(new PaymentRequest("b1", 5000));   // different amount, same key
        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(PaymentStatus.APPROVED, second.status());
    }
}
