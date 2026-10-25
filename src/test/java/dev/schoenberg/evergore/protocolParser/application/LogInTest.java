package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.schoenberg.evergore.protocolParser.FixedRandom;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class LogInTest {
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final String HASH_OF_NO_PASSWORD = "hashed:a password nobody holds";
	private static final String ADMIN_KEY = FixedRandom.ADMIN_KEY_OF_ONES;

	private final List<String> trail = new ArrayList<>();
	private final InMemoryAccounts accounts = new InMemoryAccounts(trail);
	private final PrefixHasher hasher = new PrefixHasher(trail);
	private final RecordingLogins logins = new RecordingLogins();
	private final LoginGenerations generations = new LoginGenerations();
	private final AdminKey adminKey = AdminKey.drawn(new FixedRandom());
	private final LogIn tested = new LogIn(new PasswordCheck(accounts, hasher, HASH_OF_NO_PASSWORD),
			new LoginStart(accounts, logins, generations, Clock.fixed(NOW, ZoneOffset.UTC)), adminKey);

	@ParameterizedTest
	@CsvSource({"admin", "ADMIN", "Admin"})
	void logsInTheAdminKeyUnderTheNameAdminInAnyCapitalsAsTheAdminKey(String name) {
		LogInOutcome outcome = tested.with(name, ADMIN_KEY);

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("admin-key-login-admin"));
	}

	@Test
	void writesNoLastLoginAndChecksNoAccountForTheAdminKey() {
		tested.with("admin", ADMIN_KEY);

		assertThat(trail).isEmpty();
	}

	@Test
	void refusesAnyOtherPasswordUnderTheNameAdmin() {
		LogInOutcome outcome = tested.with("admin", "Sommer-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(logins.issued()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({"Aurora", "Zephyr"})
	void refusesTheAdminKeyUnderAnyOtherName(String name) {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.ADMIN));

		LogInOutcome outcome = tested.with(name, ADMIN_KEY);

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
	}

	@Test
	void refusesTheAdminKeyWrittenInOtherCapitals() {
		LogInOutcome outcome = tested.with("admin", ADMIN_KEY.toLowerCase());

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
	}

	@Test
	void logsInAChosenPasswordWithALoginNamedAfterTheAccount() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		LogInOutcome outcome = tested.with("Aurora", "Sommer-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Aurora-MEMBER-0"));
	}

	@ParameterizedTest
	@CsvSource({"aurora", "AURORA", "Aurora"})
	void findsTheAccountWhateverCapitalsTheNameIsTypedIn(String typed) {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		LogInOutcome outcome = tested.with(typed, "Sommer-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Aurora-MEMBER-0"));
	}

	@Test
	void findsAnAccountWhoseNameHasAnUmlautRegardlessOfCapitals() {
		accounts.hold(account("Ärger", "Winter-2026!", PasswordState.CHOSEN, Role.MEMBER));

		LogInOutcome outcome = tested.with("ÄRGER", "Winter-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Ärger-MEMBER-0"));
	}

	@Test
	void givesTheLoginTheRoleOfTheAccount() {
		accounts.hold(account("Boreas", "Winter-2026!", PasswordState.CHOSEN, Role.ADMIN));

		LogInOutcome outcome = tested.with("Boreas", "Winter-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Boreas-ADMIN-0"));
	}

	@Test
	void givesTheLoginTheCurrentGenerationOfTheAccount() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));
		generations.bumpWhileAt("aurora", 0, () -> {});

		LogInOutcome outcome = tested.with("Aurora", "Sommer-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Aurora-MEMBER-1"));
	}

	@Test
	void givesTheLoginTheGenerationItSawBeforeTheCheckWhenAPasswordChangeRunsDuringIt() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));
		hasher.runDuringTheNextVerification(() -> {
			accounts.replacePassword("aurora", "hashed:Herbst-2026!", PasswordState.CHOSEN);
			generations.bumpWhileAt("aurora", 0, () -> {});
		});

		LogInOutcome outcome = tested.with("Aurora", "Sommer-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.LoggedIn("login-Aurora-MEMBER-0"));
	}

	@Test
	void recordsTheMomentOfTheLoginAsTheLastLogin() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		tested.with("Aurora", "Sommer-2026!");

		assertThat(accounts.held("aurora").lastLogin()).contains(NOW);
	}

	@Test
	void verifiesThePasswordBeforeItWritesTheLastLogin() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		tested.with("Aurora", "Sommer-2026!");

		assertThat(trail).containsExactly("verify", "recordLastLogin");
	}

	@Test
	void asksToChooseANewPasswordForAnIssuedPasswordAndIssuesNoLogin() {
		accounts.hold(account("Boreas", "Issued-2026!", PasswordState.ISSUED, Role.MEMBER));

		LogInOutcome outcome = tested.with("Boreas", "Issued-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.ChooseNewPassword());
		assertThat(logins.issued()).isEmpty();
	}

	@Test
	void recordsNoLastLoginWhereTheNewPasswordIsStillToBeChosen() {
		accounts.hold(account("Boreas", "Issued-2026!", PasswordState.ISSUED, Role.MEMBER));

		tested.with("Boreas", "Issued-2026!");

		assertThat(accounts.held("boreas").lastLogin()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({"CHOSEN", "ISSUED"})
	void refusesAWrongPasswordWithoutALoginOrALastLogin(PasswordState state) {
		accounts.hold(account("Aurora", "Sommer-2026!", state, Role.MEMBER));

		LogInOutcome outcome = tested.with("Aurora", "sOMMER-2026!");

		assertThat(outcome).isEqualTo(new LogInOutcome.Refused());
		assertThat(logins.issued()).isEmpty();
		assertThat(accounts.held("aurora").lastLogin()).isEmpty();
	}

	@Test
	void refusesAnUnknownNameWithTheAnswerOfAWrongPassword() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		LogInOutcome unknownName = tested.with("Zephyr", "Sommer-2026!");
		LogInOutcome wrongPassword = tested.with("Aurora", "Winter-2026!");

		assertThat(unknownName).isEqualTo(wrongPassword);
	}

	@Test
	void checksTheHashOfNoPasswordForAnUnknownNameSoThatItTakesTheTimeOfAWrongPassword() {
		tested.with("Zephyr", "Sommer-2026!");

		assertThat(hasher.verifications()).isEqualTo(1);
		assertThat(trail).containsExactly("verify");
	}

	@Test
	void checksOnePasswordForAWrongPasswordToo() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		tested.with("Aurora", "Winter-2026!");

		assertThat(hasher.verifications()).isEqualTo(1);
	}

	@Test
	void refusesAnEmptyNameAndAnEmptyPassword() {
		accounts.hold(account("Aurora", "Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER));

		LogInOutcome emptyName = tested.with("", "Sommer-2026!");
		LogInOutcome emptyPassword = tested.with("Aurora", "");

		assertThat(emptyName).isEqualTo(new LogInOutcome.Refused());
		assertThat(emptyPassword).isEqualTo(new LogInOutcome.Refused());
	}

	private static Account account(String avatar, String password, PasswordState state, Role role) {
		return new Account(avatar, "hashed:" + password, state, role, Optional.empty());
	}
}
