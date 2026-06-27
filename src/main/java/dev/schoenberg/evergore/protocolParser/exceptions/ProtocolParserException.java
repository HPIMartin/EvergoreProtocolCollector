package dev.schoenberg.evergore.protocolParser.exceptions;

public abstract class ProtocolParserException extends RuntimeException {

	public interface ExceptionResponseVisitor<T> {
		T onAccessNotAllowed(AccessNotAllowed exception);

		T onNoElementFound(NoElementFound exception);

		T onTooManyRequests(TooManyRequests exception);

		T onUnknown(ProtocolParserException exception);
	}

	public abstract <T> T accept(ExceptionResponseVisitor<T> visitor);
}
