package dev.schoenberg.evergore.protocolParser.helper.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("evergore.security")
public record SecurityConfiguration(String apiToken) {}
