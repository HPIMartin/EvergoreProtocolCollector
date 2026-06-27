package dev.schoenberg.evergore.protocolParser.exceptions;

public class AccessNotAllowed extends ProtocolParserException {

	@Override
	public <T> T accept(ExceptionResponseVisitor<T> visitor) {
		return visitor.onAccessNotAllowed(this);
	}
}
