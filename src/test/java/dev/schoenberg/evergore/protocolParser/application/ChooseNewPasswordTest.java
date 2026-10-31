package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRule;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseNewPasswordTest {
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final String HASH_OF_NO_PASSWORD = "hashed:a password nobody holds";
	private static final String ISSUED = "Issued-2026!";
	private static final String NEW_PASSWORD = "Sommer-2026!";

	private final List<String> trail = new ArrayList<>();
	private final InMemoryAccounts accounts = new InMemoryAccounts(trail);
	private final PrefixHasher hasher = new PrefixHasher(trail);
	private final RecordingLogins logins = new RecordingLogins();
	private final LoginGenerations generations = new LoginGenerations();
	private final LoginStart loginStart = new LoginStart(accounts, logins, generations, Clock.fixed(NOW, ZoneOffset.UTC));
	private final BankRepositoryStub bank = new BankRepositoryStub();
	private final ChooseNewPassword tested = new ChooseNewPassword(new PasswordCheck(accounts, hasher, HASH_OF_NO_PASSWORD), accounts, hasher, new PasswordRule(), loginStart,
			new RestingAccounts(bank, new StorageRepositoryStub()));

	@Test
	void logsInWithTheNewPasswordAfterTheRightIssuedOne() {
		accounts.hold(issuedAccount("Boreas"));

		LogInOutcome outcome = tested.with("boreas", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Boreas-MEMBER-0"));
	}

	@Test
	void letsTheMemberChooseThePasswordHeldBeforeAResetAgain() {
		String reissued = "Reissued-2026!";
		accounts.hold(issuedAccount("Boreas"));
		tested.with("Boreas", ISSUED, NEW_PASSWORD);
		new ResetPassword(accounts, () -> reissued, hasher, generations).forAccount("Boreas");

		LogInOutcome outcome = tested.with("Boreas", reissued, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Boreas-MEMBER-1"));
		assertThat(accounts.held("boreas").passwordHash()).isEqualTo("hashed:" + NEW_PASSWORD);
	}

	@Test
	void givesTheLoginTheGenerationItSawBeforeTheCheckWhenTheGenerationMovesDuringIt() {
		accounts.hold(issuedAccount("Boreas"));
		hasher.runDuringTheNextVerification(() -> generations.bumpWhileAt("boreas", 0, () -> {}));

		LogInOutcome outcome = tested.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Boreas-MEMBER-0"));
	}

	@Test
	void replacesTheIssuedPasswordByTheHashOfTheNewOneAsChosen() {
		accounts.hold(issuedAccount("Boreas"));

		tested.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(accounts.held("boreas")).isEqualTo(new Account("Boreas", "hashed:" + NEW_PASSWORD, PasswordState.CHOSEN, Role.MEMBER, Optional.of(NOW)));
	}

	@Test
	void hashesTheNewPasswordBeforeItsTransactionsBegin() {
		accounts.hold(issuedAccount("Boreas"));

		tested.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(trail).containsExactly("verify", "hash", "replaceIssuedPassword", "recordLastLogin");
	}

	@Test
	void refusesAWrongIssuedPasswordAsAWrongPassword() {
		accounts.hold(issuedAccount("Boreas"));

		LogInOutcome outcome = tested.with("Boreas", "Winter-2026!", NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(accounts.held("boreas")).isEqualTo(issuedAccount("Boreas"));
		assertThat(logins.issued()).isEmpty();
	}

	@Test
	void refusesAnUnknownNameAsAWrongPasswordAfterOneCheck() {
		LogInOutcome outcome = tested.with("Zephyr", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(hasher.verifications()).isEqualTo(1);
	}

	@Test
	void refusesAnAccountWhosePasswordWasAlreadyChosenAsAWrongPassword() {
		Account chosen = new Account("Aurora", "hashed:" + ISSUED, PasswordState.CHOSEN, Role.MEMBER, Optional.empty());
		accounts.hold(chosen);

		LogInOutcome outcome = tested.with("Aurora", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(accounts.held("aurora")).isEqualTo(chosen);
	}

	@Test
	void refusesAWrongIssuedPasswordBeforeItJudgesTheNewOne() {
		accounts.hold(issuedAccount("Boreas"));

		LogInOutcome outcome = tested.with("Boreas", "Winter-2026!", "Abc-123");

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Abc-123", "Abcdefg-!", "Abcd1234", "Abcd 1234", ""})
	void refusesANewPasswordThatBreaksTheRule(String newPassword) {
		accounts.hold(issuedAccount("Boreas"));

		LogInOutcome outcome = tested.with("Boreas", ISSUED, newPassword);

		assertThat(outcome).isEqualTo(new LogInOutcome.NewPasswordRefused(PasswordRefusal.BREAKS_THE_RULE));
	}

	@Test
	void refusesTheIssuedPasswordAsTheNewOne() {
		accounts.hold(issuedAccount("Boreas"));

		LogInOutcome outcome = tested.with("Boreas", ISSUED, ISSUED);

		assertThat(outcome).isEqualTo(new LogInOutcome.NewPasswordRefused(PasswordRefusal.SAME_AS_BEFORE));
	}

	@Test
	void leavesTheIssuedPasswordValidAfterARefusedChoice() {
		accounts.hold(issuedAccount("Boreas"));

		tested.with("Boreas", ISSUED, "Abc-123");

		assertThat(accounts.held("boreas")).isEqualTo(issuedAccount("Boreas"));
		assertThat(logins.issued()).isEmpty();
	}

	@Test
	void refusesTheLoserOfARaceForTheSameIssuedPasswordAsAWrongPassword() {
		Account winner = new Account("Boreas", "hashed:" + "Winner-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(NOW));
		accounts.hold(issuedAccount("Boreas"));
		PasswordHasher hasherWhoseHashIsOvertaken = new PasswordHasher() {
			@Override
			public String hash(String clear) {
				accounts.hold(winner);
				return hasher.hash(clear);
			}

			@Override
			public boolean verify(String clear, String encoded) {
				return hasher.verify(clear, encoded);
			}
		};
		ChooseNewPassword loser = new ChooseNewPassword(new PasswordCheck(accounts, hasher, HASH_OF_NO_PASSWORD), accounts, hasherWhoseHashIsOvertaken, new PasswordRule(),
				loginStart, new RestingAccounts(bank, new StorageRepositoryStub()));

		LogInOutcome outcome = loser.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(accounts.held("boreas")).isEqualTo(winner);
		assertThat(logins.issued()).isEmpty();
	}

	@Test
	void leavesThePasswordOfAResetThatOvertakesTheChoiceDuringItsCheck() {
		accounts.hold(issuedAccount("Boreas"));
		ResetPassword reset = new ResetPassword(accounts, () -> "Reissued-2026!", hasher, generations);
		hasher.runDuringTheNextVerification(() -> reset.forAccount("Boreas"));

		LogInOutcome outcome = tested.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(accounts.held("boreas").passwordHash()).isEqualTo("hashed:Reissued-2026!");
		assertThat(accounts.held("boreas").passwordState()).isEqualTo(PasswordState.ISSUED);
		assertThat(accounts.held("boreas").lastLogin()).isEmpty();
	}

	@Test
	void tellsAnAccountThatRestsSoAndLeavesItsIssuedPasswordValid() {
		accounts.hold(issuedAccount("Boreas"));
		Instant newest = Instant.parse("2026-01-31T12:00:00Z");
		bank.seedEntries("Boreas", List.of(new BankEntry(newest.minus(Duration.ofDays(31)), "Boreas", 300, TransferType.ENTNAHME)));
		bank.seedEntries("Aurora", List.of(new BankEntry(newest, "Aurora", 1000, TransferType.EINLAGERUNG)));

		LogInOutcome outcome = tested.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new LogInOutcome.Resting());
		assertThat(accounts.held("boreas")).isEqualTo(issuedAccount("Boreas"));
		assertThat(logins.issued()).isEmpty();
	}

	@Test
	void recordsNoLastLoginWhenAResetReissuesThePasswordBetweenTheChoiceAndTheStartOfItsLogin() {
		accounts.hold(issuedAccount("Boreas"));
		ResetPassword reset = new ResetPassword(accounts, () -> "Reissued-2026!", hasher, generations);
		AccountRepository resettingAfterTheChoice = new AccountsThatRunAfterTheChoice(accounts, () -> reset.forAccount("Boreas"));
		ChooseNewPassword overtaken = new ChooseNewPassword(new PasswordCheck(resettingAfterTheChoice, hasher, HASH_OF_NO_PASSWORD), resettingAfterTheChoice, hasher,
				new PasswordRule(), new LoginStart(resettingAfterTheChoice, logins, generations, Clock.fixed(NOW, ZoneOffset.UTC)),
				new RestingAccounts(bank, new StorageRepositoryStub()));

		overtaken.with("Boreas", ISSUED, NEW_PASSWORD);

		assertThat(accounts.held("boreas").lastLogin()).isEmpty();
	}

	private static Account issuedAccount(String avatar) {
		return new Account(avatar, "hashed:" + ISSUED, PasswordState.ISSUED, Role.MEMBER, Optional.empty());
	}

	private static class AccountsThatRunAfterTheChoice implements AccountRepository {
		private final AccountRepository delegate;
		private final Runnable afterTheChoice;

		AccountsThatRunAfterTheChoice(AccountRepository delegate, Runnable afterTheChoice) {
			this.delegate = delegate;
			this.afterTheChoice = afterTheChoice;
		}

		@Override
		public Optional<Account> findByNameKey(String nameKey) {
			return delegate.findByNameKey(nameKey);
		}

		@Override
		public List<Account> allAccounts() {
			return delegate.allAccounts();
		}

		@Override
		public boolean addIfAbsent(Account account) {
			return delegate.addIfAbsent(account);
		}

		@Override
		public void replacePassword(String nameKey, String passwordHash, PasswordState state) {
			delegate.replacePassword(nameKey, passwordHash, state);
		}

		@Override
		public boolean replaceIssuedPassword(String nameKey, String issuedHash, String passwordHash) {
			boolean replaced = delegate.replaceIssuedPassword(nameKey, issuedHash, passwordHash);
			if (replaced) {
				afterTheChoice.run();
			}
			return replaced;
		}

		@Override
		public void recordLastLogin(String nameKey, Instant at) {
			delegate.recordLastLogin(nameKey, at);
		}
	}
}
