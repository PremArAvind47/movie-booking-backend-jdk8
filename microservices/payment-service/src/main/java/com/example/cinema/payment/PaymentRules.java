package com.example.cinema.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * A setting that comes from the Config Server (config-repo/payment-service.yml).
 * @RefreshScope means: after POST /actuator/refresh the new value is used WITHOUT a restart.
 */
@Component
@RefreshScope
public class PaymentRules {

    private final int declineAbove;

    public PaymentRules(@Value("${payment.decline-above:1000}") int declineAbove) {
        this.declineAbove = declineAbove;
    }

    public int declineAbove() {
        return declineAbove;
    }
}
