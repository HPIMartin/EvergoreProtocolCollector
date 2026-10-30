package dev.schoenberg.evergore.protocolParser.acceptance.operator;

public record Asking(String method, String address) {
	private static final String GET = "GET";
	private static final String POST = "POST";

	public static Asking get(String address) {
		return new Asking(GET, address);
	}

	public static Asking post(String address) {
		return new Asking(POST, address);
	}

	public boolean isPost() {
		return POST.equals(method);
	}
}
