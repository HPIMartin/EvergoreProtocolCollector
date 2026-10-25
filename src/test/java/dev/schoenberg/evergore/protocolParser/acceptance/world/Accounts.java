package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;

import static org.assertj.core.api.Assertions.assertThat;

public class Accounts {
	private static final int SERVED = 200;

	private final ScenarioPasswords passwords;
	private final LoginRequests logins;
	private final Map<String, String> chosen = new ConcurrentHashMap<>();

	public Accounts(ScenarioPasswords passwords, LoginRequests logins) {
		this.passwords = passwords;
		this.logins = logins;
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

	public void choosePasswordInPlaceOfTheIssuedOne(String avatar, String password) {
		Answer answer = logins.chooseNewPassword(avatar, issuedTo(avatar), password);

		assertThat(answer.status()).as("the choice of %s's password: %s", avatar, answer.body()).isEqualTo(SERVED);
		chosen.put(avatar, password);
	}
}
