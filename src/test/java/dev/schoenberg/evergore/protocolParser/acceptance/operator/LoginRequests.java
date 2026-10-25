package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

public class LoginRequests {
	public static final String SESSION_COOKIE = "epc_session";

	private static final String LOGIN = "/api/v1/login";
	private static final String NEW_PASSWORD = "/api/v1/login/new-password";
	private static final String SET_COOKIE = "Set-Cookie";
	private static final String ADDRESS_PREFIX = "10.1.";
	private static final int ADDRESSES_PER_BLOCK = 250;
	private static final ObjectMapper JSON = new ObjectMapper();

	private final ServiceRequests requests;
	private int addressesUsed;

	public LoginRequests(ServiceRequests requests) {
		this.requests = requests;
	}

	public Answer logIn(String name, String password) {
		return requests.post(LOGIN, json(Map.of("name", name, "password", password)), fromAFreshAddress());
	}

	public Answer logIn(String name, String password, String next) {
		return requests.post(LOGIN, json(Map.of("name", name, "password", password, "next", next)), fromAFreshAddress());
	}

	public static String nextIn(Answer answer) {
		try {
			return JSON.readTree(answer.body()).path("next").asText();
		} catch (JsonProcessingException e) {
			throw new AssertionError("The login's answer is not JSON: " + answer.body(), e);
		}
	}

	public Answer chooseNewPassword(String name, String issuedPassword, String newPassword) {
		return requests.post(NEW_PASSWORD, json(Map.of("name", name, "issuedPassword", issuedPassword, "newPassword", newPassword)), fromAFreshAddress());
	}

	public Answer asking(String address, String loginValue) {
		Map<String, String> headers = new HashMap<>(fromAFreshAddress());
		headers.put("Cookie", SESSION_COOKIE + "=" + loginValue);
		return requests.get(address, headers);
	}

	public static String loginValueIn(Answer answer) {
		String cookie = answer.header(SET_COOKIE).orElseThrow(() -> new AssertionError("The service set no cookie, it answered " + answer.status() + ": " + answer.body()));
		String pair = cookie.split(";", 2)[0];
		if (!pair.startsWith(SESSION_COOKIE + "=")) {
			throw new AssertionError("The service set another cookie than the login: " + cookie);
		}
		return pair.substring(SESSION_COOKIE.length() + 1);
	}

	private Map<String, String> fromAFreshAddress() {
		int used = addressesUsed++;
		return Map.of(AcceptanceClientIp.HEADER, ADDRESS_PREFIX + used / ADDRESSES_PER_BLOCK + "." + (used % ADDRESSES_PER_BLOCK + 1));
	}

	private static String json(Map<String, String> fields) {
		try {
			return JSON.writeValueAsString(fields);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
