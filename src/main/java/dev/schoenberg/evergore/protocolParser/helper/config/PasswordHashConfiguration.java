package dev.schoenberg.evergore.protocolParser.helper.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

@ConfigurationProperties("evergore.password-hash")
public record PasswordHashConfiguration(@Bindable(defaultValue = "65536") int memoryKibibytes, @Bindable(defaultValue = "3") int iterations,
		@Bindable(defaultValue = "1") int parallelism, @Bindable(defaultValue = "2") int maxConcurrent) {}
