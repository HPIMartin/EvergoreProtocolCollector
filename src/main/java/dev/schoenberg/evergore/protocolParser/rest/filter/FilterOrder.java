package dev.schoenberg.evergore.protocolParser.rest.filter;

enum FilterOrder {
	REQUEST_AUDIT_LOG(1),
	RATE_LIMIT(2),
	TOKEN_VALIDATION(3);

	private final int position;

	FilterOrder(int position) {
		this.position = position;
	}

	int position() {
		return position;
	}
}
