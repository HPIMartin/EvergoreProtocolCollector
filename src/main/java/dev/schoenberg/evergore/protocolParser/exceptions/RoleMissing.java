package dev.schoenberg.evergore.protocolParser.exceptions;

public class RoleMissing extends ProtocolParserException {

	@Override
	public <T> T accept(ExceptionResponseVisitor<T> visitor) {
		return visitor.onRoleMissing(this);
	}
}
