package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.time.Instant;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {
	private static final String CHOOSING_A_NEW_PASSWORD = "Neues Passwort wählen";

	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final Accounts accounts;
	private final ScenarioTime time;
	private Instant loggedInAt;
	private String nameEntered;

	public LoginSteps(MemberBrowser browser, SecondBrowser secondBrowser, Accounts accounts, ScenarioTime time) {
		this.time = time;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.accounts = accounts;
	}

	@ParameterType("\\d+ days?")
	public Duration loginAge(String age) {
		return Duration.ofDays(Long.parseLong(age.split(" ")[0]));
	}

	@When("the member opens the overview {loginAge} after logging in")
	public void theMemberOpensTheOverviewAfterLoggingIn(Duration age) {
		time.setTo(loggedInAt.plus(age));
		browser.openHref(OVERVIEW);
	}

	@When("a/the member logs in as {string} with the password {string}")
	public void aMemberLogsInWithThePassword(String name, String password) {
		logIn(name, password);
	}

	@Given("the member has tried to log in as {string} with the password {string}")
	public void theMemberHasTriedToLogInWithThePassword(String name, String password) {
		logIn(name, password);
	}

	@Given("a/the member has logged in as {string} with the password {string}")
	public void aMemberHasLoggedInWithThePassword(String name, String password) {
		logIn(name, password);
	}

	@Given("the admin has logged in as {string} with the password {string}")
	public void theAdminHasLoggedInWithThePassword(String name, String password) {
		logIn(name, password);
	}

	@Given("a member has logged in as {string} with the password {string} on two browsers")
	public void aMemberHasLoggedInOnTwoBrowsers(String name, String password) {
		logIn(name, password);
		secondBrowser.browser().loginForm().logIn(name, password);
	}

	@When("a/the member/admin logs in as {string} with the password the (first collection's )reset issued")
	public void aMemberLogsInWithThePasswordTheResetIssued(String name) {
		logIn(name, accounts.issuedTo(name));
	}

	@When("a/the member logs in as {string} with the password the reset issued to {word}")
	public void aMemberLogsInWithThePasswordTheResetIssuedTo(String name, String avatar) {
		logIn(name, accounts.issuedTo(avatar));
	}

	@Given("a member has logged in as {string} after choosing {string} in place of the password the reset issued")
	public void aMemberHasLoggedInAfterChoosing(String name, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(name, password);
		logIn(name, password);
	}

	@Given("a/the member has entered the name {string} and the password the reset issued on the login page")
	public void aMemberHasEnteredTheNameAndTheIssuedPassword(String name) {
		logIn(name, accounts.issuedTo(name));

		assertThat(browser.ledger().heading()).isEqualTo(CHOOSING_A_NEW_PASSWORD);
	}

	@When("the member chooses {string} as the new password")
	public void theMemberChoosesTheNewPassword(String newPassword) {
		browser.loginForm().chooseTheNewPassword(newPassword, newPassword);
	}

	@When("the member chooses {string} as the new password but repeats it as {string}")
	public void theMemberChoosesTheNewPasswordButRepeatsItAs(String newPassword, String repeated) {
		browser.loginForm().chooseTheNewPassword(newPassword, repeated);
	}

	@When("the member chooses the issued password as the new password")
	public void theMemberChoosesTheIssuedPasswordAsTheNewPassword() {
		String issued = accounts.issuedTo(nameEntered);
		browser.loginForm().chooseTheNewPassword(issued, issued);
	}

	private void logIn(String name, String password) {
		nameEntered = name;
		loggedInAt = time.now();
		browser.loginForm().logIn(name, password);
	}
}
