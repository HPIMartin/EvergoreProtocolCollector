package dev.schoenberg.evergore.protocolParser.helper.config;

import java.util.List;
import java.util.Optional;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("evergore.security")
public record SecurityConfiguration(Optional<String> apiToken, List<String> publicPaths) {}
