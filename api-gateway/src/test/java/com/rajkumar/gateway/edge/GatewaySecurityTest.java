package com.rajkumar.gateway.edge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * Security rules at the edge. Downstream services point at a closed port, so a request
 * that gets past security ends in a 5xx from the proxy, while a rejected one is a
 * 401/403 produced by the gateway itself, without any downstream call.
 */
@SpringBootTest(properties = {
        "app.security.jwt.secret=" + TestTokens.SECRET,
        "app.gateway.auth-service-uri=http://localhost:1",
        "app.gateway.catalog-service-uri=http://localhost:1",
        "app.gateway.rate-limit.enabled=false",
        "management.health.redis.enabled=false"
})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient client;

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
    }

    @Test
    void missingTokenIs401() {
        client.get().uri("/api/v1/products").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists("WWW-Authenticate");
    }

    @Test
    void tamperedTokenIs401() {
        String token = TestTokens.token("alice", "USER");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        client.get().uri("/api/v1/products").headers(h -> h.setBearerAuth(tampered)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void expiredTokenIs401() {
        String expired = TestTokens.token("alice", TestTokens.ISSUER, Instant.now().minus(Duration.ofHours(1)), "USER");
        client.get().uri("/api/v1/products").headers(h -> h.setBearerAuth(expired)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenFromAnotherIssuerIs401() {
        String foreign = TestTokens.token("alice", "evil-issuer", Instant.now().plus(Duration.ofMinutes(5)), "ADMIN");
        client.get().uri("/api/v1/products").headers(h -> h.setBearerAuth(foreign)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void userCanReadCatalogueSoRequestIsForwarded() {
        client.get().uri("/api/v1/products").headers(h -> h.setBearerAuth(TestTokens.token("alice", "USER"))).exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void userCannotWriteCatalogue() {
        client.post().uri("/api/v1/products").headers(h -> h.setBearerAuth(TestTokens.token("alice", "USER")))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminCanWriteCatalogueSoRequestIsForwarded() {
        client.post().uri("/api/v1/products").headers(h -> h.setBearerAuth(TestTokens.token("root", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void adminApiNeedsAdminRole() {
        client.get().uri("/api/v1/admin/users").headers(h -> h.setBearerAuth(TestTokens.token("alice", "USER"))).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void loginIsPublicSoRequestIsForwarded() {
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"alice\",\"password\":\"x\"}").exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void unknownPathsAreDenied() {
        client.get().uri("/internal/anything").headers(h -> h.setBearerAuth(TestTokens.token("root", "ADMIN"))).exchange()
                .expectStatus().isForbidden();
    }
}
