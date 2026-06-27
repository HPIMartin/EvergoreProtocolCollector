package dev.schoenberg.evergore.protocolParser.businessLogic.base;

public enum TransferType {
	EINLAGERUNG {
		@Override
		public <T> T accept(TransferTypeVisitor<T> visitor) {
			return visitor.place();
		}

		@Override
		public String toGermanString() {
			return "Einlagerung";
		}
	},
	ENTNAHME {
		@Override
		public <T> T accept(TransferTypeVisitor<T> visitor) {
			return visitor.withdrawl();
		}

		@Override
		public String toGermanString() {
			return "Entnahme";
		}
	};

	public abstract <T> T accept(TransferTypeVisitor<T> visitor);

	public abstract String toGermanString();

	public static interface TransferTypeVisitor<T> {
		T place();

		T withdrawl();
	}
}
