package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.util.Locale;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountSteps {
	private final Accounts accounts;
	private final ScenarioTime time;
	private final MemberBrowser browser;

	public AccountSteps(Accounts accounts, ScenarioTime time, MemberBrowser browser) {
		this.accounts = accounts;
		this.time = time;
		this.browser = browser;
	}

	@Given("the admin has reset the password of every account")
	public void theAdminHasResetThePasswordOfEveryAccount() {
		accounts.resetEveryAccount();
	}

	@Given("{word} has chosen the password {string}")
	public void hasChosenThePassword(String avatar, String password) {
		accounts.chooseDirectly(avatar, password);
	}

	@Given("{word} has chosen {string} in place of the password the reset issued")
	public void hasChosenInPlaceOfThePasswordTheResetIssued(String avatar, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(avatar, password);
	}

	@Given("{word} has chosen {string} in place of the password the reset issued and logged in with it again {int} hours later")
	public void hasChosenInPlaceOfThePasswordTheResetIssuedAndLoggedInAgainLater(String avatar, String password, int hours) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(avatar, password);
		time.passes(Duration.ofHours(hours));
		browser.clock().reads(time.now());
		accounts.logIn(avatar);
	}

	@Given("{word}'s account has the role {string}")
	public void accountHasTheRole(String avatar, String role) {
		accounts.giveTheRole(avatar, Role.valueOf(role.toUpperCase(Locale.ROOT)));
	}

	@Given("{word}'s account holds the password she/he chose, {string}")
	public void accountHoldsThePasswordSheChose(String avatar, String password) {
		assertThat(accounts.chosenBy(avatar)).isEqualTo(password);
	}

	@Given("{word}'s account still holds the password the reset issued")
	public void accountStillHoldsThePasswordTheResetIssued(String avatar) {
		accounts.issuedTo(avatar);
	}
}
