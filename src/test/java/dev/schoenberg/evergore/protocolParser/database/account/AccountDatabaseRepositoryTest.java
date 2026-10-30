package dev.schoenberg.evergore.protocolParser.database.account;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AccountDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/accountRepositoryTest.sqlite";
	private static final Instant LOGGED_IN = Instant.parse("2026-01-02T12:00:00.123Z");

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private SqliteDatabase database;
	private AccountDatabaseRepository tested;

	@BeforeEach
	void openAFreshDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		database = SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy());
		tested = new AccountDatabaseRepository(database);
	}

	@AfterEach
	void closeTheDatabase() {
		database.close();
	}

	@Test
	void findsNoAccountByANameKeyNobodyHolds() {
		Optional<Account> found = tested.findByNameKey("aurora");

		assertThat(found).isEmpty();
	}

	@Test
	void findsAnAddedAccountByItsFoldedName() {
		Account added = account("Ärger", "hash-1", PasswordState.ISSUED);
		tested.addIfAbsent(added);

		Optional<Account> found = tested.findByNameKey("ärger");

		assertThat(found).contains(added);
	}

	@Test
	void keepsTheAvatarAsTheLedgersWriteIt() {
		tested.addIfAbsent(account("Ärger", "hash-1", PasswordState.ISSUED));

		Optional<String> avatar = tested.findByNameKey("ärger").map(Account::avatar);

		assertThat(avatar).contains("Ärger");
	}

	@Test
	void reportsTheAccountAddedWhenNoneHeldTheName() {
		boolean added = tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));

		assertThat(added).isTrue();
	}

	@Test
	void addsNothingAndKeepsTheFirstAccountWhenOneHoldsTheNameKeyAlready() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.CHOSEN));

		boolean added = tested.addIfAbsent(account("AURORA", "hash-2", PasswordState.ISSUED));

		assertThat(added).isFalse();
		assertThat(tested.findByNameKey("aurora")).contains(account("Aurora", "hash-1", PasswordState.CHOSEN));
	}

	@Test
	void replacesThePasswordHashAndItsStateAndNothingElse() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));
		tested.recordLastLogin("aurora", LOGGED_IN);

		tested.replacePassword("aurora", "hash-2", PasswordState.CHOSEN);

		assertThat(tested.findByNameKey("aurora")).contains(new Account("Aurora", "hash-2", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));
	}

	@Test
	void replacesThePasswordOfTheNamedAccountOnly() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));
		tested.addIfAbsent(account("Boreas", "hash-1", PasswordState.ISSUED));

		tested.replacePassword("aurora", "hash-2", PasswordState.CHOSEN);

		assertThat(tested.findByNameKey("boreas")).contains(account("Boreas", "hash-1", PasswordState.ISSUED));
	}

	@Test
	void replacesAnIssuedPasswordByTheChosenOneAndKeepsTheLastLogin() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));
		tested.recordLastLogin("aurora", LOGGED_IN);

		boolean replaced = tested.replaceIssuedPassword("aurora", "hash-2");

		assertThat(replaced).isTrue();
		assertThat(tested.findByNameKey("aurora")).contains(new Account("Aurora", "hash-2", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));
	}

	@Test
	void replacesNoChosenPasswordAsAnIssuedOne() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.CHOSEN));

		boolean replaced = tested.replaceIssuedPassword("aurora", "hash-2");

		assertThat(replaced).isFalse();
		assertThat(tested.findByNameKey("aurora")).contains(account("Aurora", "hash-1", PasswordState.CHOSEN));
	}

	@Test
	void replacesTheIssuedPasswordOfTheNamedAccountOnly() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));
		tested.addIfAbsent(account("Boreas", "hash-1", PasswordState.ISSUED));

		tested.replaceIssuedPassword("aurora", "hash-2");

		assertThat(tested.findByNameKey("boreas")).contains(account("Boreas", "hash-1", PasswordState.ISSUED));
	}

	@Test
	void replacesNothingForANameNobodyHolds() {
		boolean replaced = tested.replaceIssuedPassword("nobody", "hash-2");

		assertThat(replaced).isFalse();
	}

	@Test
	void recordsTheLastLoginAsAnInstantDownToTheMillisecond() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.CHOSEN));

		tested.recordLastLogin("aurora", LOGGED_IN);

		assertThat(tested.findByNameKey("aurora").flatMap(Account::lastLogin)).contains(LOGGED_IN);
	}

	@Test
	void storesTheLastLoginAsUtcEpochMillis() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.CHOSEN));

		tested.recordLastLogin("aurora", LOGGED_IN);

		assertThat(databaseFile.committedTexts("account", "last_login_epoch_millis")).containsExactly(String.valueOf(LOGGED_IN.toEpochMilli()));
	}

	@Test
	void answersNoLastLoginForAnAccountThatNeverLoggedIn() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));

		Optional<Instant> lastLogin = tested.findByNameKey("aurora").flatMap(Account::lastLogin);

		assertThat(lastLogin).isEmpty();
	}

	@Test
	void readsTheAdminRoleAGrantByHandSets() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.CHOSEN));
		databaseFile.execute("UPDATE account SET role = 'ADMIN' WHERE name_key = 'aurora'");

		Optional<Role> role = tested.findByNameKey("aurora").map(Account::role);

		assertThat(role).contains(Role.ADMIN);
	}

	@Test
	void givesARowInsertedWithoutARoleTheMemberRole() {
		databaseFile
				.execute(
						"INSERT INTO account (id, avatar, name_key, password_hash, password_state) VALUES ('00000000-0000-0000-0000-000000000001', 'Aurora', 'aurora', 'hash-1', 'ISSUED')");

		Optional<Role> role = tested.findByNameKey("aurora").map(Account::role);

		assertThat(role).contains(Role.MEMBER);
	}

	@Test
	void refusesTwoRowsOfOneNameKey() {
		databaseFile
				.execute(
						"INSERT INTO account (id, avatar, name_key, password_hash, password_state) VALUES ('00000000-0000-0000-0000-000000000001', 'Aurora', 'aurora', 'hash-1', 'ISSUED')");

		Throwable refusal = catchThrowable(() -> databaseFile
				.execute(
						"INSERT INTO account (id, avatar, name_key, password_hash, password_state) VALUES ('00000000-0000-0000-0000-000000000002', 'AURORA', 'aurora', 'hash-2', 'ISSUED')"));

		assertThat(refusal).hasMessageContaining("UNIQUE constraint failed");
	}

	@Test
	void refusesARoleOutsideTheTwoItKnows() {
		Throwable refusal = catchThrowable(() -> databaseFile
				.execute(
						"INSERT INTO account (id, avatar, name_key, password_hash, password_state, role) VALUES ('00000000-0000-0000-0000-000000000001', 'Aurora', 'aurora', 'hash-1', 'ISSUED', 'OWNER')"));

		assertThat(refusal).hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void keepsEveryColumnButTheLastLoginFromBeingNull() {
		Throwable refusal = catchThrowable(() -> databaseFile
				.execute(
						"INSERT INTO account (id, avatar, name_key, password_hash, password_state) VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'aurora', 'hash-1', 'ISSUED')"));

		assertThat(refusal).hasMessageContaining("NOT NULL constraint failed");
	}

	@Test
	void listsEveryStoredAccountWithItsRoleAndLatestLogin() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));
		tested.addIfAbsent(account("Boreas", "hash-2", PasswordState.CHOSEN));
		databaseFile.execute("UPDATE account SET role = 'ADMIN' WHERE name_key = 'boreas'");
		tested.recordLastLogin("boreas", LOGGED_IN);

		List<Account> all = tested.allAccounts();

		assertThat(all)
				.containsExactlyInAnyOrder(account("Aurora", "hash-1", PasswordState.ISSUED),
						new Account("Boreas", "hash-2", PasswordState.CHOSEN, Role.ADMIN, Optional.of(LOGGED_IN)));
	}

	@Test
	void findsNoAccountOfAnotherNameKey() {
		tested.addIfAbsent(account("Aurora", "hash-1", PasswordState.ISSUED));

		List<Optional<Account>> found = List.of(tested.findByNameKey("boreas"), tested.findByNameKey("aurora "));

		assertThat(found).containsOnly(Optional.empty());
	}

	private static Account account(String avatar, String hash, PasswordState state) {
		return new Account(avatar, hash, state, Role.MEMBER, Optional.empty());
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}
}
