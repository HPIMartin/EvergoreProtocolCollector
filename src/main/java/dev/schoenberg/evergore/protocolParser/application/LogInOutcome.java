package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;

public sealed interface LogInOutcome {
	<T> T accept(Visitor<T> visitor);

	interface Visitor<T> {
		T onLoggedIn(String loginValue);

		T onChooseNewPassword();

		T onRefused();

		T onResting();

		T onNewPasswordRefused(PasswordRefusal refusal);
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

	record Resting() implements LogInOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onResting();
		}
	}

	record NewPasswordRefused(PasswordRefusal refusal) implements LogInOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onNewPasswordRefused(refusal);
		}
	}
}
