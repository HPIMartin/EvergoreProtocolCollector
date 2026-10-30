package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

public class Accounts {
	private static final String ADMIN = "admin";
	private static final int SERVED = 200;
	private static final int NO_CONTENT = 204;
	private static final int BUSY_MILLIS = 5000;

	private final ScenarioPasswords passwords;
	private final LoginRequests logins;
	private final ScenarioDatabase database;
	private final ServiceLog log;
	private final RunningService service;
	private final Map<String, String> chosen = new ConcurrentHashMap<>();

	public Accounts(ScenarioPasswords passwords, LoginRequests logins, ScenarioDatabase database, ServiceLog log, RunningService service) {
		this.passwords = passwords;
		this.logins = logins;
		this.database = database;
		this.log = log;
		this.service = service;
	}

	public String issuedTo(String avatar) {
		return passwords.issuedTo(avatar).orElseThrow(() -> new AssertionError("No reset has issued a password to " + avatar));
	}

	public String firstIssuedTo(String avatar) {
		return passwords.firstIssuedTo(avatar).orElseThrow(() -> new AssertionError("No reset has issued a password to " + avatar));
	}

	public void resetEveryAccount() {
		String adminLogin = adminLogin();
		Answer accounts = logins.listAccounts(adminLogin);

		assertThat(accounts.status()).as("the list of accounts: %s", accounts.body()).isEqualTo(SERVED);
		LoginRequests.namesIn(accounts).forEach(avatar -> resetWith(adminLogin, avatar));
	}

	public void reset(String avatar) {
		resetWith(adminLogin(), avatar);
	}

	public void chooseDirectly(String avatar, String password) {
		service.storeTheChosenPassword(avatar, password);
		chosen.put(avatar, password);
	}

	private void resetWith(String adminLogin, String avatar) {
		Answer answer = logins.resetPassword(adminLogin, avatar);

		assertThat(answer.status()).as("the reset of %s's password: %s", avatar, answer.body()).isEqualTo(SERVED);
		passwords.record(avatar, LoginRequests.passwordIn(answer));
	}

	private String adminLogin() {
		return LoginRequests.loginValueIn(logins.logIn(ADMIN, log.adminKeyNamed()));
	}

	public String chosenBy(String avatar) {
		String password = chosen.get(avatar);
		if (password == null) {
			throw new AssertionError(avatar + " has chosen no password in this scenario");
		}
		return password;
	}

	public void changePassword(String avatar, String current, String replacement) {
		String login = LoginRequests.loginValueIn(logins.logIn(avatar, current));
		Answer answer = logins.changePassword(login, Optional.of(current), replacement);

		assertThat(answer.status()).as("the change of %s's password: %s", avatar, answer.body()).isEqualTo(NO_CONTENT);
		chosen.put(avatar, replacement);
	}

	public void rememberTheChange(String avatar, String replacement) {
		chosen.put(avatar, replacement);
	}

	public void giveTheRole(String avatar, Role role) {
		String url = "jdbc:sqlite:" + database.file() + "?busy_timeout=" + BUSY_MILLIS;
		try (Connection connection = DriverManager.getConnection(url); PreparedStatement update = connection.prepareStatement("UPDATE account SET role = ? WHERE name_key = ?")) {
			update.setString(1, role.name());
			update.setString(2, new LoginName(avatar).key());
			assertThat(update.executeUpdate()).as("the accounts of %s", avatar).isEqualTo(1);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public void logIn(String avatar) {
		Answer answer = logins.logIn(avatar, chosenBy(avatar));

		assertThat(answer.status()).as("the login of %s: %s", avatar, answer.body()).isEqualTo(SERVED);
	}

	public void choosePasswordInPlaceOfTheIssuedOne(String avatar, String password) {
		Answer answer = logins.chooseNewPassword(avatar, issuedTo(avatar), password);

		assertThat(answer.status()).as("the choice of %s's password: %s", avatar, answer.body()).isEqualTo(SERVED);
		chosen.put(avatar, password);
	}
}
