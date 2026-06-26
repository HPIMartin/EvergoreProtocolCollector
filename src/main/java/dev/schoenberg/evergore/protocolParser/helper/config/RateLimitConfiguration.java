package dev.schoenberg.evergore.protocolParser.helper.config;

import java.time.Duration;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("evergore.rate-limit")
public record RateLimitConfiguration(long maxRequestsPerInterval, Duration interval, Duration blockDuration) {}
