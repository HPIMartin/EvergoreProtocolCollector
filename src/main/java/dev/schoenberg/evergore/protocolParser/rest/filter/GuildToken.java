package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.security.MessageDigest;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;

import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
public class GuildToken {
	private static final String TOKEN_PARAMETER_NAME = "token";

	private final SecurityConfiguration securityConfiguration;

	public GuildToken(SecurityConfiguration securityConfiguration) {
		this.securityConfiguration = securityConfiguration;
	}

	public boolean opens(HttpRequest<?> request) {
		return request.getParameters().get(TOKEN_PARAMETER_NAME, String.class).filter(this::matchesApiToken).isPresent();
	}

	private boolean matchesApiToken(String presented) {
		return securityConfiguration
				.apiToken()
				.filter(expected -> !expected.isBlank())
				.map(expected -> MessageDigest.isEqual(presented.getBytes(UTF_8), expected.getBytes(UTF_8)))
				.orElse(false);
	}
}
