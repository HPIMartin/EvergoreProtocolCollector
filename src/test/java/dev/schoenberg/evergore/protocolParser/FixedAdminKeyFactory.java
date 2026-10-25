package dev.schoenberg.evergore.protocolParser;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import dev.schoenberg.evergore.protocolParser.application.AdminKey;

@Factory
@Requires(property = FixedAdminKeyFactory.FIXED_ADMIN_KEY)
public class FixedAdminKeyFactory {
	public static final String FIXED_ADMIN_KEY = "test.fixed-admin-key";

	@Singleton
	@Replaces(AdminKey.class)
	AdminKey fixedAdminKey() {
		return AdminKey.drawn(new FixedRandom());
	}
}
