package com.example.cinema.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an id at the front door. The id is sent to the service, comes back in the
 * response header, and is written in the access log line. Services copy it into their logs.
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);
    // an id from the outside is accepted only if it is safe (no log injection)
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming != null && SAFE.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString().substring(0, 8);

        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, id).build();
        // set just before the response is sent, so the header appears exactly once
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });

        long start = System.currentTimeMillis();
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("[{}] {} {} -> {} in {} ms", id,
                        exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(),
                        exchange.getResponse().getStatusCode(), System.currentTimeMillis() - start));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
