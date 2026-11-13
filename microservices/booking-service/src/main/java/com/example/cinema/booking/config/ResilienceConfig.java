package com.example.cinema.booking.config;

import com.example.cinema.platform.ApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker rules for every call to another service.
 * After enough failures the circuit OPENS: calls fail at once (no waiting) so one broken service
 * does not make everything slow. After a pause it lets a few calls through to test if it is healthy again.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerCustomizer(
            @Value("${resilience.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${resilience.minimum-calls:5}") int minimumCalls,
            @Value("${resilience.open-wait-seconds:10}") int openWaitSeconds,
            @Value("${resilience.timeout-seconds:5}") int timeoutSeconds) {

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(minimumCalls)
                        .failureRateThreshold(failureRateThreshold)
                        .waitDurationInOpenState(Duration.ofSeconds(openWaitSeconds))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        // a business answer such as "not enough seats" is not a failure of the other service
                        .ignoreExceptions(ApiException.class)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(timeoutSeconds))
                        .build())
                .build());
    }
}
