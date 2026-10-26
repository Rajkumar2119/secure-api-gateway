package com.rajkumar.gateway.edge.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String forwardedId(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwarded.set(ex);
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        String id = forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER);
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER)).isEqualTo(id);
        return id;
    }

    @Test
    void generatesIdWhenMissing() {
        String id = forwardedId(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")));

        assertThat(id).matches("[0-9a-f-]{36}");
    }

    @Test
    void keepsWellFormedClientId() {
        String id = forwardedId(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(RequestIdFilter.HEADER, "client-trace-12345")));

        assertThat(id).isEqualTo("client-trace-12345");
    }

    @Test
    void replacesMalformedClientId() {
        String id = forwardedId(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(RequestIdFilter.HEADER, "<script>alert(1)</script>")));

        assertThat(id).isNotEqualTo("<script>alert(1)</script>").matches("[0-9a-f-]{36}");
    }
}
