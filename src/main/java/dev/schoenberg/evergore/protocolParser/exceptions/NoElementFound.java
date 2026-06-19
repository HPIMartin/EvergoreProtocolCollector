package dev.schoenberg.evergore.protocolParser.exceptions;

public class NoElementFound extends ProtocolParserException {
	public final String requestedValue;

	public NoElementFound(String requestedValue) {
		this.requestedValue = requestedValue;
	}

	@Override
	public String getMessage() {
		return requestedValue;
	}
}
