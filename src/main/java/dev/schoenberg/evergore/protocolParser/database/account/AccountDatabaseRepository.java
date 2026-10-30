package dev.schoenberg.evergore.protocolParser.database.account;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;

import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.database.account.AccountEntry.LAST_LOGIN_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.account.AccountEntry.NAME_KEY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.account.AccountEntry.PASSWORD_HASH_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.account.AccountEntry.PASSWORD_STATE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class AccountDatabaseRepository implements AccountRepository {
	private final SqliteDatabase database;

	public AccountDatabaseRepository(SqliteDatabase database) {
		this.database = database;
	}

	@Override
	public Optional<Account> findByNameKey(String nameKey) {
		return entryOf(nameKey).map(AccountDatabaseRepository::accountOf);
	}

	@Override
	public List<Account> allAccounts() {
		return silentThrow(() -> accounts().queryForAll()).stream().map(AccountDatabaseRepository::accountOf).toList();
	}

	@Override
	public boolean addIfAbsent(Account account) {
		return database.inTransaction(() -> {
			if (entryOf(account.nameKey()).isPresent()) {
				return false;
			}
			accounts().create(entryOf(account));
			return true;
		});
	}

	@Override
	public void replacePassword(String nameKey, String passwordHash, PasswordState state) {
		database.inTransaction(() -> {
			UpdateBuilder<AccountEntry, String> update = accounts().updateBuilder();
			update.updateColumnValue(PASSWORD_HASH_COLUMN, passwordHash).updateColumnValue(PASSWORD_STATE_COLUMN, state).where().eq(NAME_KEY_COLUMN, nameKey);
			return update.update();
		});
	}

	@Override
	public boolean replaceIssuedPassword(String nameKey, String passwordHash) {
		return database.inTransaction(() -> {
			UpdateBuilder<AccountEntry, String> update = accounts().updateBuilder();
			update
					.updateColumnValue(PASSWORD_HASH_COLUMN, passwordHash)
					.updateColumnValue(PASSWORD_STATE_COLUMN, PasswordState.CHOSEN)
					.where()
					.eq(NAME_KEY_COLUMN, nameKey)
					.and()
					.eq(PASSWORD_STATE_COLUMN, PasswordState.ISSUED);
			return update.update() > 0;
		});
	}

	@Override
	public void recordLastLogin(String nameKey, Instant at) {
		database.inTransaction(() -> {
			UpdateBuilder<AccountEntry, String> update = accounts().updateBuilder();
			update.updateColumnValue(LAST_LOGIN_COLUMN, at.toEpochMilli()).where().eq(NAME_KEY_COLUMN, nameKey);
			return update.update();
		});
	}

	private Optional<AccountEntry> entryOf(String nameKey) {
		return silentThrow(() -> accounts().queryBuilder().where().eq(NAME_KEY_COLUMN, nameKey).query()).stream().findFirst();
	}

	private Dao<AccountEntry, String> accounts() {
		return database.dao(AccountEntry.class);
	}

	private static AccountEntry entryOf(Account account) {
		return new AccountEntry(account.avatar(), account.nameKey(), account.passwordHash(), account.passwordState(), account.role(),
				account.lastLogin().map(Instant::toEpochMilli).orElse(null));
	}

	private static Account accountOf(AccountEntry entry) {
		return new Account(entry.avatar, entry.passwordHash, entry.passwordState, entry.role, Optional.ofNullable(entry.lastLoginEpochMillis).map(Instant::ofEpochMilli));
	}
}
