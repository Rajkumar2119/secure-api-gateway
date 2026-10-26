package com.rajkumar.gateway.edge.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.security.Principal;

/**
 * Redis token-bucket rate limiting (Spring Cloud Gateway's RedisRateLimiter runs a Lua
 * script, so the limit holds across every gateway replica). Callers over the limit get
 * 429 Too Many Requests with X-RateLimit-* headers.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    RedisRateLimiter apiRateLimiter(EdgeProperties properties) {
        EdgeProperties.RateLimit rl = properties.rateLimit();
        return new RedisRateLimiter(rl.apiReplenishRate(), rl.apiBurstCapacity(), 1);
    }

    @Bean
    RedisRateLimiter authRateLimiter(EdgeProperties properties) {
        EdgeProperties.RateLimit rl = properties.rateLimit();
        return new RedisRateLimiter(rl.authReplenishRate(), rl.authBurstCapacity(), 1);
    }

    /** Signed-in callers are limited per user; anonymous calls (login, register) per client IP. */
    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .map(name -> "user:" + name)
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
    }

    /** Auth endpoints are always limited per IP, so one address can't hammer login with many usernames. */
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + clientIp(exchange));
    }

    /**
     * First address in X-Forwarded-For when a load balancer sits in front, otherwise the
     * socket address. Only trust X-Forwarded-For when the gateway is behind your own proxy.
     */
    static String clientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null) {
            return "unknown";
        }
        return remote.getAddress() != null ? remote.getAddress().getHostAddress() : remote.getHostString();
    }
}
