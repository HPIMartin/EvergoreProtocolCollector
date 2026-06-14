package dev.schoenberg.evergore.protocolParser.businessLogic.base;

public enum TransferType {
	EINLAGERUNG {
		@Override
		public <T> T accept(TransferTypeVisitor<T> visitor) {
			return visitor.place();
		}
	},
	ENTNAHME {
		@Override
		public <T> T accept(TransferTypeVisitor<T> visitor) {
			return visitor.withdrawl();
		}
	};

	public abstract <T> T accept(TransferTypeVisitor<T> visitor);

	public static interface TransferTypeVisitor<T> {
		T place();

		T withdrawl();
	}
}
