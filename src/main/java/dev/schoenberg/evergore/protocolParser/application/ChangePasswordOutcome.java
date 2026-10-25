package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;

public sealed interface ChangePasswordOutcome {
	<T> T accept(Visitor<T> visitor);

	interface Visitor<T> {
		T onChanged(String loginValue);

		T onNoAccount();

		T onLoginEnded();

		T onWrongCurrentPassword();

		T onNewPasswordRefused(PasswordRefusal refusal);
	}

	record Changed(String loginValue) implements ChangePasswordOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onChanged(loginValue);
		}
	}

	record NoAccount() implements ChangePasswordOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onNoAccount();
		}
	}

	record LoginEnded() implements ChangePasswordOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onLoginEnded();
		}
	}

	record WrongCurrentPassword() implements ChangePasswordOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onWrongCurrentPassword();
		}
	}

	record NewPasswordRefused(PasswordRefusal refusal) implements ChangePasswordOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onNewPasswordRefused(refusal);
		}
	}
}
