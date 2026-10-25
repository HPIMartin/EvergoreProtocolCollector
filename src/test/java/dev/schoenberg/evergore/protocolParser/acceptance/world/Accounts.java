package dev.schoenberg.evergore.protocolParser.acceptance.world;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;

import static org.assertj.core.api.Assertions.assertThat;

public class Accounts {
	private static final int SERVED = 200;

	private final ScenarioPasswords passwords;
	private final LoginRequests logins;

	public Accounts(ScenarioPasswords passwords, LoginRequests logins) {
		this.passwords = passwords;
		this.logins = logins;
	}

	public String issuedTo(String avatar) {
		return passwords.issuedTo(avatar).orElseThrow(() -> new AssertionError("No collection has issued a password to " + avatar));
	}

	public void choosePasswordInPlaceOfTheIssuedOne(String avatar, String password) {
		Answer chosen = logins.chooseNewPassword(avatar, issuedTo(avatar), password);

		assertThat(chosen.status()).as("the choice of %s's password: %s", avatar, chosen.body()).isEqualTo(SERVED);
	}
}
