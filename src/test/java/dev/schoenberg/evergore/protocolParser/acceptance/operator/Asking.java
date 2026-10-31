package dev.schoenberg.evergore.protocolParser.acceptance.operator;

public record Asking(String method, String address, String body) {
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String NO_BODY = "";

	public static Asking get(String address) {
		return new Asking(GET, address, NO_BODY);
	}

	public static Asking post(String address) {
		return new Asking(POST, address, NO_BODY);
	}

	public static Asking post(String address, String jsonBody) {
		return new Asking(POST, address, jsonBody);
	}

	public boolean isPost() {
		return POST.equals(method);
	}
}
