package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

class InMemoryAccounts implements AccountRepository {
	private final Map<String, Account> byNameKey = new LinkedHashMap<>();
	private final List<String> trail;

	InMemoryAccounts(List<String> trail) {
		this.trail = trail;
	}

	void hold(Account account) {
		byNameKey.put(account.nameKey(), account);
	}

	Account held(String nameKey) {
		return byNameKey.get(nameKey);
	}

	@Override
	public Optional<Account> findByNameKey(String nameKey) {
		return Optional.ofNullable(byNameKey.get(nameKey));
	}

	@Override
	public boolean addIfAbsent(Account account) {
		return byNameKey.putIfAbsent(account.nameKey(), account) == null;
	}

	@Override
	public void replacePassword(String nameKey, String passwordHash, PasswordState state) {
		trail.add("replacePassword");
		Account held = byNameKey.get(nameKey);
		byNameKey.put(nameKey, new Account(held.avatar(), passwordHash, state, held.role(), held.lastLogin()));
	}

	@Override
	public void recordLastLogin(String nameKey, Instant at) {
		trail.add("recordLastLogin");
		Account held = byNameKey.get(nameKey);
		byNameKey.put(nameKey, new Account(held.avatar(), held.passwordHash(), held.passwordState(), held.role(), Optional.of(at)));
	}
}
