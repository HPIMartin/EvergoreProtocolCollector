package dev.schoenberg.evergore.protocolParser.helper.config;

import java.time.Duration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

@ConfigurationProperties("evergore.usage-writing")
public record UsageWritingConfiguration(@Bindable(defaultValue = "10000") int capacity, @Bindable(defaultValue = "500") int batchSize,
		@Bindable(defaultValue = "10s") Duration closeBound) {}
