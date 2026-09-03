package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.time.ZoneId;
import java.util.Map;

import jakarta.inject.Singleton;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class TimezoneStartupValidatorTest {
	private final LoggerSpy logger = new LoggerSpy();

	@Test
	void aZoneObservingDaylightSavingStopsStartup() {
		TimezoneStartupValidator tested = validatorFor(ZoneId.of("Europe/Berlin"));

		assertThatThrownBy(tested::validateTimezone).isInstanceOf(IllegalStateException.class).hasMessageContaining("Europe/Berlin");
	}

	@Test
	void utcAllowsStartup() {
		TimezoneStartupValidator tested = validatorFor(ZoneId.of("UTC"));

		assertThatNoException().isThrownBy(tested::validateTimezone);
	}

	@Test
	void aNonUtcFixedOffsetZoneAllowsStartup() {
		ZoneId fixedOffsetZone = ZoneId.of("Etc/GMT-2");
		assertThat(fixedOffsetZone.getRules().isFixedOffset()).isTrue();

		TimezoneStartupValidator tested = validatorFor(fixedOffsetZone);

		assertThatNoException().isThrownBy(tested::validateTimezone);
	}

	@Test
	void failureIsLoggedWithTheZoneName() {
		TimezoneStartupValidator tested = validatorFor(ZoneId.of("Europe/Berlin"));

		assertThatThrownBy(tested::validateTimezone).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).singleElement().asString().contains("Europe/Berlin");
	}

	@Test
	void onApplicationEventPropagatesFailureWhenZoneObservesDaylightSaving() {
		TimezoneStartupValidator tested = validatorFor(ZoneId.of("Europe/Berlin"));

		assertThatThrownBy(() -> tested.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("Europe/Berlin");
	}

	@Test
	void theRealSystemDefaultZoneAllowsTheApplicationContextToStart() {
		Map<String, Object> validLogin = Map.of("evergore.credentials.username", "username", "evergore.credentials.password", "password", "micronaut.server.port", "-1");

		Throwable thrown = catchThrowable(() -> ApplicationContext.run(validLogin, "test").close());

		assertThat(thrown).isNull();
	}

	@Test
	void aDstObservingZoneBeanStopsTheApplicationContextFromStarting() {
		Map<String, Object> validLoginWithDstZoneSpec = Map
				.of("spec.name", "TimezoneStartupValidatorTest.dstZone", "evergore.credentials.username", "username", "evergore.credentials.password", "password",
						"micronaut.server.port", "-1");

		Throwable thrown = catchThrowable(() -> ApplicationContext.run(validLoginWithDstZoneSpec, "test").close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Europe/Berlin");
	}

	private TimezoneStartupValidator validatorFor(ZoneId zone) {
		return new TimezoneStartupValidator(zone, logger);
	}

	@Factory
	@Requires(property = "spec.name", value = "TimezoneStartupValidatorTest.dstZone")
	static class DstZoneTestFactory {
		@Singleton
		@Replaces(bean = ZoneId.class)
		ZoneId dstObservingZone() {
			return ZoneId.of("Europe/Berlin");
		}
	}
}
