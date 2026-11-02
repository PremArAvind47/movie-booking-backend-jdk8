package com.example.moviebooking.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Part 16: runs around EVERY request.
// 1. gives the request a short number, and puts it on every log line (see %X{requestId} in logback-spring.xml)
// 2. sends the number back to the caller in the "X-Request-Id" header
// 3. logs one summary line when the request is finished: method, path, status and how long it took
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);                      // MDC = a note that every log line on this thread can read
        response.setHeader("X-Request-Id", requestId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);                // the real work happens here
        } finally {
            log.info("REQUEST {} {} -> {} in {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - start);
            MDC.remove("requestId");
        }
    }
}
