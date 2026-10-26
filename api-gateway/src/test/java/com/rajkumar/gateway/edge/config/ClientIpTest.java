package com.rajkumar.gateway.edge.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpTest {

    @Test
    void usesFirstForwardedForAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"));

        assertThat(RateLimitConfig.clientIp(exchange)).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToRemoteAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("198.51.100.4", 50000)));

        assertThat(RateLimitConfig.clientIp(exchange)).isEqualTo("198.51.100.4");
    }

    @Test
    void anonymousKeyResolverUsesIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "203.0.113.9"));

        assertThat(new RateLimitConfig().userOrIpKeyResolver().resolve(exchange).block()).isEqualTo("ip:203.0.113.9");
    }
}
