package dev.schoenberg.evergore.protocolParser.exceptions;

public class TooManyRequests extends ProtocolParserException {
	private final String identifier;

	public TooManyRequests(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String getMessage() {
		return identifier;
	}
}
