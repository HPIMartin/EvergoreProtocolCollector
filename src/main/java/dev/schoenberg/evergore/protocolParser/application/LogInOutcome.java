package dev.schoenberg.evergore.protocolParser.application;

public sealed interface LogInOutcome {
	<T> T accept(Visitor<T> visitor);

	interface Visitor<T> {
		T onLoggedIn(String loginValue);

		T onChooseNewPassword();

		T onRefused();
	}

	record LoggedIn(String loginValue) implements LogInOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onLoggedIn(loginValue);
		}
	}

	record ChooseNewPassword() implements LogInOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onChooseNewPassword();
		}
	}

	record Refused() implements LogInOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onRefused();
		}
	}
}
