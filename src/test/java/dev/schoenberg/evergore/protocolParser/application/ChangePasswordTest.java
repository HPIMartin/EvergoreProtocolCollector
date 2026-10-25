package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRule;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordTest {
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final String HASH_OF_NO_PASSWORD = "hashed:a password nobody holds";
	private static final String CURRENT = "Sommer-2026!";
	private static final String NEW_PASSWORD = "Winter-2026!";
	private static final String ANOTHER = "Herbst-2026!";
	private static final Login AURORA = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", NOW, NOW.plus(Login.LIFETIME));
	private static final Login THE_ADMIN_KEY = new Login("admin", Role.ADMIN, 0, LoginSource.ADMIN_KEY, "key-id", NOW, NOW.plus(Login.LIFETIME));

	private final List<String> trail = new ArrayList<>();
	private final InMemoryAccounts accounts = new InMemoryAccounts(trail);
	private final PrefixHasher hasher = new PrefixHasher(trail);
	private final RecordingLogins logins = new RecordingLogins();
	private final LoginGenerations generations = new LoginGenerations();
	private final ChangePassword tested = new ChangePassword(new PasswordCheck(accounts, hasher, HASH_OF_NO_PASSWORD), accounts, hasher, new PasswordRule(), generations, logins);

	@Test
	void startsALoginOfTheNewGenerationAfterTheRightCurrentPassword() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.Changed("login-Aurora-MEMBER-1"));
	}

	@Test
	void keepsTheRoleOfTheLoginItChangesOnWhateverTheAccountHoldsNow() {
		accounts.hold(new Account("Aurora", "hashed:" + CURRENT, PasswordState.CHOSEN, Role.ADMIN, Optional.empty()));

		ChangePasswordOutcome outcome = tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.Changed("login-Aurora-MEMBER-1"));
	}

	@Test
	void endsTheNewLoginWhenTheLoginItReplacesEnds() {
		Login replaced = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", NOW.minus(Duration.ofDays(6)), NOW.plus(Duration.ofDays(1)));
		accounts.hold(chosenAccount("Aurora", CURRENT));

		tested.with(replaced, CURRENT, NEW_PASSWORD);

		assertThat(logins.lastExpiry()).contains(replaced.expiresAt());
	}

	@Test
	void recordsNoLastLoginBecauseAChangeIsNoLogin() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(accounts.held("aurora").lastLogin()).isEmpty();
	}

	@Test
	void replacesThePasswordByTheHashOfTheNewOneAsChosen() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(accounts.held("aurora")).isEqualTo(new Account("Aurora", "hashed:" + NEW_PASSWORD, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));
	}

	@Test
	void endsTheOtherLoginsOfTheAccountOnly() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(List.of(generations.of("aurora"), generations.of("boreas"))).containsExactly(1L, 0L);
	}

	@Test
	void hashesTheNewPasswordBeforeItsTransactionsBegin() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		tested.with(AURORA, CURRENT, NEW_PASSWORD);

		assertThat(trail).containsExactly("verify", "hash", "replacePassword");
	}

	@ParameterizedTest
	@ValueSource(strings = {ANOTHER, ""})
	void refusesAWrongOrMissingCurrentPasswordAndChangesNothing(String current) {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(AURORA, current, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.WrongCurrentPassword());
		assertThat(accounts.held("aurora")).isEqualTo(chosenAccount("Aurora", CURRENT));
		assertThat(List.of(generations.of("aurora"), logins.issued().size())).containsExactly(0L, 0);
	}

	@Test
	void refusesAWrongCurrentPasswordBeforeItJudgesTheNewOne() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(AURORA, ANOTHER, "Abc-123");

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.WrongCurrentPassword());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Abc-123", "Abcdefg-!", "Abcd1234", "Abcd 1234", ""})
	void refusesANewPasswordThatBreaksTheRule(String newPassword) {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(AURORA, CURRENT, newPassword);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.NewPasswordRefused(PasswordRefusal.BREAKS_THE_RULE));
		assertThat(accounts.held("aurora")).isEqualTo(chosenAccount("Aurora", CURRENT));
	}

	@Test
	void refusesTheCurrentPasswordAsTheNewOne() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(AURORA, CURRENT, CURRENT);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.NewPasswordRefused(PasswordRefusal.SAME_AS_BEFORE));
		assertThat(generations.of("aurora")).isZero();
	}

	@Test
	void refusesAChangeWhoseLoginAnotherChangeEndedWhileItWasChecked() {
		accounts.hold(chosenAccount("Aurora", CURRENT));
		Login owners = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "owner", NOW, NOW.plus(Login.LIFETIME));
		List<ChangePasswordOutcome> ownersOutcome = new ArrayList<>();
		hasher.runDuringTheNextVerification(() -> ownersOutcome.add(tested.with(owners, CURRENT, NEW_PASSWORD)));

		ChangePasswordOutcome attackers = tested.with(AURORA, CURRENT, ANOTHER);

		assertThat(ownersOutcome).containsExactly(new ChangePasswordOutcome.Changed("login-Aurora-MEMBER-1"));
		assertThat(accounts.held("aurora").passwordHash()).isEqualTo("hashed:" + NEW_PASSWORD);
		assertThat(attackers).isEqualTo(new ChangePasswordOutcome.LoginEnded());
	}

	@Test
	void leavesTheGenerationOfTheChangeThatWonUntouchedWhenAnotherOneLoses() {
		accounts.hold(chosenAccount("Aurora", CURRENT));
		Login owners = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "owner", NOW, NOW.plus(Login.LIFETIME));
		hasher.runDuringTheNextVerification(() -> tested.with(owners, CURRENT, NEW_PASSWORD));

		ChangePasswordOutcome loser = tested.with(AURORA, CURRENT, ANOTHER);

		long generation = generations.of("aurora");

		assertThat(loser).isEqualTo(new ChangePasswordOutcome.LoginEnded());
		assertThat(generation).isEqualTo(1L);
	}

	@Test
	void refusesTheLoginOfTheAdminKeyForLackingAnAccountWithoutAnyCheck() {
		accounts.hold(chosenAccount("Aurora", CURRENT));

		ChangePasswordOutcome outcome = tested.with(THE_ADMIN_KEY, CURRENT, NEW_PASSWORD);

		assertThat(outcome).isEqualTo(new ChangePasswordOutcome.NoAccount());
		assertThat(hasher.verifications()).isZero();
	}

	private static Account chosenAccount(String avatar, String password) {
		return new Account(avatar, "hashed:" + password, PasswordState.CHOSEN, Role.MEMBER, Optional.empty());
	}
}
