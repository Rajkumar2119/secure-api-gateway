package com.rajkumar.gateway.edge.filter;

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
 * Gives every request an {@code X-Request-Id} (keeps a well-formed one sent by the client),
 * forwards it to the service and returns it in the response, so one id ties together the
 * gateway log line, the service logs and what the client saw.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, requestId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, requestId);
        long start = System.nanoTime();

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("{} {} -> {} in {} ms [{}]",
                        request.getMethod(), request.getPath().value(), exchange.getResponse().getStatusCode(),
                        (System.nanoTime() - start) / 1_000_000, requestId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
