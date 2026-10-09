package dev.schoenberg.evergore.protocolParser.helper.config;

import java.util.Optional;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("evergore.credentials")
public record CredentialsConfiguration(Optional<String> username, Optional<String> password) {}
