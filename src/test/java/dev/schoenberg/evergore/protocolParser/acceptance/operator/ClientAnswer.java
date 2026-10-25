package dev.schoenberg.evergore.protocolParser.acceptance.operator;

public class ClientAnswer {
	private Answer answer;

	public void remember(Answer received) {
		answer = received;
	}

	public Answer current() {
		if (answer == null) {
			throw new IllegalStateException("No client has asked the service anything yet");
		}
		return answer;
	}
}
