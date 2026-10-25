package dev.schoenberg.evergore.protocolParser.monitoring;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.management.health.indicator.HealthResult;

@Factory
public class HealthReportJsonModule {
	@Singleton
	Module healthReportWithoutUnknownParts() {
		return new SimpleModule().setMixInAnnotation(HealthResult.class, OnlyWhatIsKnown.class);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonPropertyOrder({"status", "details", "name"})
	abstract static class OnlyWhatIsKnown {}
}
