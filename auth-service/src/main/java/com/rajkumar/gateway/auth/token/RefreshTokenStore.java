package com.rajkumar.gateway.auth.token;

import java.time.Duration;
import java.util.Optional;

/** Server-side store of refresh tokens. Only a SHA-256 hash of each token is kept. */
public interface RefreshTokenStore {

    void save(String tokenHash, long userId, Duration ttl);

    /** Atomically reads and deletes the token, so it can be used exactly once. */
    Optional<Long> consume(String tokenHash);

    void revoke(String tokenHash);
}
