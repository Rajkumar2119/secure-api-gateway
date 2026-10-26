package com.rajkumar.gateway.catalog.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajkumar.gateway.catalog.api.CategoryResponse;
import com.rajkumar.gateway.catalog.api.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.List;

/**
 * Redis cache for read-heavy catalogue data. Each cache gets a serializer typed to
 * its value class, so cached JSON comes back as the right Java type (records are
 * final, which rules out Jackson's default typing).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS = "products";
    public static final String CATEGORIES = "categories";

    @Bean
    RedisCacheManagerBuilderCustomizer catalogCaches(ObjectMapper objectMapper,
                                                     @Value("${app.cache.product-ttl:10m}") Duration productTtl,
                                                     @Value("${app.cache.category-ttl:1h}") Duration categoryTtl) {
        JavaType categoryList = objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryResponse.class);
        return builder -> builder
                .withCacheConfiguration(PRODUCTS, RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(productTtl)
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(objectMapper, ProductResponse.class))))
                .withCacheConfiguration(CATEGORIES, RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(categoryTtl)
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(objectMapper, categoryList))));
    }
}
