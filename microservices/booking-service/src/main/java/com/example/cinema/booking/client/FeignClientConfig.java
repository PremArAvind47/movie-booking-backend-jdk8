package com.example.cinema.booking.client;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * Settings for the Feign clients. It has no @Configuration on purpose: only the Feign clients
 * that name it use it (so it does not leak into the rest of the application).
 */
public class FeignClientConfig {

    /**
     * Try again once when the connection fails. This is SAFE only because every call is idempotent:
     * reserve and pay carry a key (the booking id), and release can be repeated.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(200, 1000, 2);   // wait 0.2 s, at most 2 tries in total
    }

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new DownstreamErrorDecoder(objectMapper);
    }
}
