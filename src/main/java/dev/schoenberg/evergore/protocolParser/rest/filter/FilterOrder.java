package dev.schoenberg.evergore.protocolParser.rest.filter;

enum FilterOrder {
	REQUEST_AUDIT_LOG(1),
	LOGIN_READING(2),
	RATE_LIMIT(3),
	UNREADABLE_REQUEST(4),
	ACCESS(5),
	LOGIN_BODY_LIMIT(6);

	private final int position;

	FilterOrder(int position) {
		this.position = position;
	}

	int position() {
		return position;
	}
}
