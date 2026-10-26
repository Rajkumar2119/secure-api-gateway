package com.rajkumar.gateway.edge.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Routes, defined in Java so they are type-checked and easy to test.
 * Both services keep the /api/v1 prefix, so paths are forwarded unchanged.
 */
@Configuration
public class RoutesConfig {

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder, EdgeProperties properties,
                        @Qualifier("apiRateLimiter") RedisRateLimiter apiRateLimiter,
                        @Qualifier("authRateLimiter") RedisRateLimiter authRateLimiter,
                        @Qualifier("userOrIpKeyResolver") KeyResolver userOrIpKeyResolver,
                        @Qualifier("ipKeyResolver") KeyResolver ipKeyResolver) {
        boolean limit = properties.rateLimit().enabled();
        String auth = properties.authServiceUri().toString();
        String catalog = properties.catalogServiceUri().toString();

        return builder.routes()
                // Public auth endpoints: strict per-IP limit against brute force.
                .route("auth-public", r -> r.path("/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout")
                        .and().method(HttpMethod.POST)
                        .filters(f -> common(f, limit, authRateLimiter, ipKeyResolver))
                        .uri(auth))
                .route("auth-me", r -> r.path("/api/v1/auth/me")
                        .filters(f -> common(f, limit, apiRateLimiter, userOrIpKeyResolver))
                        .uri(auth))
                .route("auth-admin", r -> r.path("/api/v1/admin/**")
                        .filters(f -> common(f, limit, apiRateLimiter, userOrIpKeyResolver))
                        .uri(auth))
                .route("catalog", r -> r.path("/api/v1/products/**", "/api/v1/categories/**")
                        .filters(f -> common(f, limit, apiRateLimiter, userOrIpKeyResolver))
                        .uri(catalog))
                .build();
    }

    private static UriSpec common(GatewayFilterSpec f, boolean limit, RedisRateLimiter limiter, KeyResolver keyResolver) {
        // Internal headers must only ever be set by the gateway, never by a client.
        f.removeRequestHeader("X-User-Id").removeRequestHeader("X-User-Roles");
        if (limit) {
            f.requestRateLimiter(c -> c.setRateLimiter(limiter).setKeyResolver(keyResolver).setDenyEmptyKey(true));
        }
        return f;
    }
}
