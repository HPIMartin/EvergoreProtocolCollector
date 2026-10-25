package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

public class Accounts {
	private static final int SERVED = 200;
	private static final int BUSY_MILLIS = 5000;

	private final ScenarioPasswords passwords;
	private final LoginRequests logins;
	private final ScenarioDatabase database;
	private final Map<String, String> chosen = new ConcurrentHashMap<>();

	public Accounts(ScenarioPasswords passwords, LoginRequests logins, ScenarioDatabase database) {
		this.passwords = passwords;
		this.logins = logins;
		this.database = database;
	}

	public String issuedTo(String avatar) {
		return passwords.issuedTo(avatar).orElseThrow(() -> new AssertionError("No collection has issued a password to " + avatar));
	}

	public String chosenBy(String avatar) {
		String password = chosen.get(avatar);
		if (password == null) {
			throw new AssertionError(avatar + " has chosen no password in this scenario");
		}
		return password;
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

	public void choosePasswordInPlaceOfTheIssuedOne(String avatar, String password) {
		Answer answer = logins.chooseNewPassword(avatar, issuedTo(avatar), password);

		assertThat(answer.status()).as("the choice of %s's password: %s", avatar, answer.body()).isEqualTo(SERVED);
		chosen.put(avatar, password);
	}
}
