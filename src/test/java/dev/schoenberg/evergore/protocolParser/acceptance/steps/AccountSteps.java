package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Locale;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountSteps {
	private final Accounts accounts;

	public AccountSteps(Accounts accounts) {
		this.accounts = accounts;
	}

	@Given("{word} has chosen the password {string}")
	public void hasChosenThePassword(String avatar, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(avatar, password);
	}

	@Given("{word} has chosen {string} in place of the password the reset issued")
	public void hasChosenInPlaceOfThePasswordTheResetIssued(String avatar, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(avatar, password);
	}

	@Given("{word}'s account has the role {string}")
	public void accountHasTheRole(String avatar, String role) {
		accounts.giveTheRole(avatar, Role.valueOf(role.toUpperCase(Locale.ROOT)));
	}

	@Given("{word}'s account holds the password she chose, {string}")
	public void accountHoldsThePasswordSheChose(String avatar, String password) {
		assertThat(accounts.chosenBy(avatar)).isEqualTo(password);
	}

	@Given("{word}'s account still holds the password the reset issued")
	public void accountStillHoldsThePasswordTheResetIssued(String avatar) {
		accounts.issuedTo(avatar);
	}
}
