package dev.schoenberg.evergore.protocolParser.exceptions;

public class NoElementFound extends ProtocolParserException {
	private static final long serialVersionUID = -396655600807040704L;

	public final String requestedValue;

	public NoElementFound(String requestedValue) {
		this.requestedValue = requestedValue;
	}

	@Override
	public String getMessage() {
		return requestedValue;
	}
}
