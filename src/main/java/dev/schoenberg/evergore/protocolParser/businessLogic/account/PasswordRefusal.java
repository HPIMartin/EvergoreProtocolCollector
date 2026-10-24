package dev.schoenberg.evergore.protocolParser.businessLogic.account;

public enum PasswordRefusal {
	BREAKS_THE_RULE {
		@Override
		public <T> T accept(PasswordRefusalVisitor<T> visitor) {
			return visitor.breaksTheRule();
		}
	},
	SAME_AS_BEFORE {
		@Override
		public <T> T accept(PasswordRefusalVisitor<T> visitor) {
			return visitor.sameAsBefore();
		}
	};

	public abstract <T> T accept(PasswordRefusalVisitor<T> visitor);

	public interface PasswordRefusalVisitor<T> {
		T breaksTheRule();

		T sameAsBefore();
	}
}
