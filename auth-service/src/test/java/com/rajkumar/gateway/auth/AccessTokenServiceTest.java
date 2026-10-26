package com.rajkumar.gateway.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rajkumar.gateway.auth.config.JwtProperties;
import com.rajkumar.gateway.auth.domain.Role;
import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.token.AccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123";

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtProperties properties = new JwtProperties(SECRET, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(7));

    private User user() {
        User user = new User("alice", "alice@example.com", "hash",
                Set.of(new Role((short) 1, RoleName.USER), new Role((short) 2, RoleName.ADMIN)));
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private NimbusJwtDecoder decoder(String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Test
    void tokenCarriesSubjectUserIdRolesAndExpiry() {
        AccessTokenService service = new AccessTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties,
                Clock.systemUTC());

        Jwt jwt = decoder("test-issuer").decode(service.issue(user()));

        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(((Number) jwt.getClaim("uid")).longValue()).isEqualTo(42L);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "USER");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(service.accessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        AccessTokenService service = new AccessTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties,
                anHourAgo);

        String token = service.issue(user());

        assertThatThrownBy(() -> decoder("test-issuer").decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        AccessTokenService service = new AccessTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties,
                Clock.systemUTC());

        String token = service.issue(user());

        assertThatThrownBy(() -> decoder("someone-else").decode(token)).isInstanceOf(JwtException.class);
    }
}
