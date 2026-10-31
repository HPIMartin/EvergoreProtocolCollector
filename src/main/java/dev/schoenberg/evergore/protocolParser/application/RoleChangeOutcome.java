package dev.schoenberg.evergore.protocolParser.application;

public sealed interface RoleChangeOutcome {
	<T> T accept(Visitor<T> visitor);

	interface Visitor<T> {
		T onChanged();
	}

	record Changed() implements RoleChangeOutcome {
		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.onChanged();
		}
	}
}
