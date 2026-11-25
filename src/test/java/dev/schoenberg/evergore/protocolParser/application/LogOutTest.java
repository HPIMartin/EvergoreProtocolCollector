package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static org.assertj.core.api.Assertions.assertThat;

class LogOutTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");

	private final PassingClock clock = new PassingClock(MADE);
	private final RecordedUsage usage = new RecordedUsage();
	private final LogOut tested = new LogOut(clock, usage);

	@Test
	void endsTheLoginThatLoggedOut() {
		Login login = loginWith("first", MADE);

		tested.of(login);

		assertThat(tested.hasEnded(login)).isTrue();
	}

	@Test
	void leavesAnotherLoginOfTheSameAccountAlone() {
		Login first = loginWith("first", MADE);
		Login second = loginWith("second", MADE);

		tested.of(first);

		assertThat(tested.hasEnded(second)).isFalse();
	}

	@Test
	void endsNothingBeforeAnyoneLoggedOut() {
		Login login = loginWith("first", MADE);

		assertThat(tested.hasEnded(login)).isFalse();
	}

	@Test
	void forgetsALoggedOutLoginWhenTheNextLogoutComesAfterItsExpiry() {
		Login first = loginWith("first", MADE);
		tested.of(first);
		clock.passes(Login.LIFETIME.plusSeconds(1));

		tested.of(loginWith("second", clock.instant()));

		assertThat(tested.hasEnded(first)).isFalse();
	}

	@Test
	void keepsALoggedOutLoginUntilItsExpiry() {
		Login first = loginWith("first", MADE);
		tested.of(first);
		clock.passes(Login.LIFETIME.minusSeconds(1));

		tested.of(loginWith("second", clock.instant()));

		assertThat(tested.hasEnded(first)).isTrue();
	}

	@Test
	void recordsALogoutOfTheAccountAtTheMomentOfTheClockUnderItsFoldedName() {
		clock.passes(Duration.ofMinutes(5));

		tested.of(new Login("Ärger", Role.MEMBER, 0, LoginSource.ACCOUNT, "first", MADE, MADE.plus(Login.LIFETIME)));

		assertThat(usage.records()).containsExactly(new UsageRecord("ärger", MADE.plus(Duration.ofMinutes(5)), UsageKind.LOGOUT, Optional.empty()));
	}

	private static Login loginWith(String id, Instant madeAt) {
		return new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, id, madeAt, madeAt.plus(Login.LIFETIME));
	}

	private static final class PassingClock extends Clock {
		private Instant now;

		private PassingClock(Instant start) {
			now = start;
		}

		private void passes(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}
	}
}
