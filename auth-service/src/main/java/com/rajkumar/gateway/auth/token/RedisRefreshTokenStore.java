package com.rajkumar.gateway.auth.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Keys are {@code refresh:<sha256>} with the user id as value and a TTL, so expired
 * tokens disappear on their own. {@code GETDEL} makes rotation atomic: two concurrent
 * refreshes with the same token can't both succeed.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String tokenHash, long userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + tokenHash, Long.toString(userId), ttl);
    }

    @Override
    public Optional<Long> consume(String tokenHash) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(PREFIX + tokenHash)).map(Long::valueOf);
    }

    @Override
    public void revoke(String tokenHash) {
        redis.delete(PREFIX + tokenHash);
    }
}
