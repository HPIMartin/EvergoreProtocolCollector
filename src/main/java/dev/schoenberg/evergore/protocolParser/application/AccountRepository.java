package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public interface AccountRepository {
	Optional<Account> findByNameKey(String nameKey);

	List<Account> allAccounts();

	boolean addIfAbsent(Account account);

	void replacePassword(String nameKey, String passwordHash, PasswordState state);

	boolean replaceIssuedPassword(String nameKey, String passwordHash);

	void recordLastLogin(String nameKey, Instant at);
}
