package com.rajkumar.gateway.edge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * @param authServiceUri    where auth-service runs
 * @param catalogServiceUri where catalog-service runs
 * @param rateLimit         token-bucket settings, applied per user (or per IP when anonymous)
 */
@ConfigurationProperties(prefix = "app.gateway")
public record EdgeProperties(URI authServiceUri, URI catalogServiceUri, RateLimit rateLimit) {

    public EdgeProperties {
        if (rateLimit == null) {
            rateLimit = new RateLimit(true, 20, 40, 5, 10);
        }
    }

    /**
     * @param enabled            switch rate limiting off (tests, local debugging)
     * @param apiReplenishRate   tokens per second added to each caller's bucket on the API routes
     * @param apiBurstCapacity   bucket size on the API routes
     * @param authReplenishRate  stricter limit on login/register/refresh to slow down credential stuffing
     * @param authBurstCapacity  bucket size on the auth routes
     */
    public record RateLimit(boolean enabled, int apiReplenishRate, int apiBurstCapacity,
                            int authReplenishRate, int authBurstCapacity) {
    }
}
