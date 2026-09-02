package com.example.rate_limiter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitLuaScript;

    public RateLimiterService(StringRedisTemplate redisTemplate, RedisScript<Long> rateLimitLuaScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitLuaScript = rateLimitLuaScript;
    }

    public boolean isAllowed(String clientKey, int maxLimit, int windowInSeconds) {
        String redisKey = "rate_limit:" + clientKey;
        long currentTimeMillis = System.currentTimeMillis();
        String requestId = currentTimeMillis + "-" + UUID.randomUUID();

        Long result = redisTemplate.execute(
                rateLimitLuaScript,
                Collections.singletonList(redisKey),
                String.valueOf(currentTimeMillis),
                String.valueOf(windowInSeconds),
                String.valueOf(maxLimit),
                requestId
        );

        return result != null && result == 1L;
    }
}