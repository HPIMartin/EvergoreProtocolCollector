package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

class RecordingLogins implements SignedLogins {
	private final List<String> issued = new ArrayList<>();
	private final Map<String, Login> readable = new HashMap<>();
	private final List<Instant> expiries = new ArrayList<>();

	List<String> issued() {
		return issued;
	}

	@Override
	public String issue(String accountName, Role role, long generation) {
		String value = "login-" + accountName + "-" + role + "-" + generation;
		issued.add(value);
		return value;
	}

	@Override
	public String issue(String accountName, Role role, long generation, Instant expiresAt) {
		expiries.add(expiresAt);
		return issue(accountName, role, generation);
	}

	Optional<Instant> lastExpiry() {
		return expiries.stream().reduce((first, second) -> second);
	}

	@Override
	public String issueForTheAdminKey(String name) {
		String value = "admin-key-login-" + name;
		issued.add(value);
		return value;
	}

	void readAs(String value, Login login) {
		readable.put(value, login);
	}

	@Override
	public Optional<Login> read(String value) {
		return Optional.ofNullable(readable.get(value));
	}
}
