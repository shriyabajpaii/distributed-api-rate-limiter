package com.example.rate_limiter.controller;

import com.example.rate_limiter.service.RateLimiterService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RateLimiterService rateLimiterService;

    public ApiController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/test")
    @CircuitBreaker(name = "redisBackend", fallbackMethod = "fallbackResponse")
    public ResponseEntity<String> testEndpoint(@RequestHeader(value = "X-USER-ID", defaultValue = "guest") String userId) {
        boolean allowed = rateLimiterService.isAllowed(userId, 3, 10);

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded! Maximum 3 requests per 10 seconds allowed.");
        }

        return ResponseEntity.ok("Request successful for user: " + userId);
    }

    public ResponseEntity<String> fallbackResponse(String userId, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Redis or Downstream issue detected. Request handled by Resilience4j Fallback.");
    }
}