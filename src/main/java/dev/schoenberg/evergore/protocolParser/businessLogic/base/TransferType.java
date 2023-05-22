package dev.schoenberg.evergore.protocolParser.businessLogic.base;

public enum TransferType {
	Einlagerung {
		@Override
		public <T> T accept(TransfertTypeVisitor<T> visitor) {
			return visitor.place();
		}
	},
	Entnahme {
		@Override
		public <T> T accept(TransfertTypeVisitor<T> visitor) {
			return visitor.withdrawl();
		}
	};

	public abstract <T> T accept(TransfertTypeVisitor<T> visitor);

	public static interface TransfertTypeVisitor<T> {
		T place();

		T withdrawl();
	}
}
