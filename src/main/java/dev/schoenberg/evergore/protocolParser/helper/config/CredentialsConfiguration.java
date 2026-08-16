package dev.schoenberg.evergore.protocolParser.helper.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("evergore.credentials")
public record CredentialsConfiguration(String username, String password) {}
