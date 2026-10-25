package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;

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

	@Given("{word}'s account still holds the password the reset issued")
	public void accountStillHoldsThePasswordTheResetIssued(String avatar) {
		accounts.issuedTo(avatar);
	}
}
