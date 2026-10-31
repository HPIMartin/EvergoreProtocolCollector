package dev.schoenberg.evergore.protocolParser.application;

public sealed interface RoleChangeOutcome {
	<T> T accept(Visitor<T> visitor);

	interface Visitor<T> {
		T onChanged();

		T onUnchanged();

		T onNoAccount();
	}

	record Changed() implements RoleChangeOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onChanged();
		}
	}

	record Unchanged() implements RoleChangeOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onUnchanged();
		}
	}

	record NoAccount() implements RoleChangeOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onNoAccount();
		}
	}
}
