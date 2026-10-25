package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ViewedBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class PasswordChangeSteps {
	private static final int SERVED = 200;

	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final Accounts accounts;
	private final LoginRequests logins;
	private final ViewedBrowser viewed;

	public PasswordChangeSteps(MemberBrowser browser, SecondBrowser secondBrowser, Accounts accounts, LoginRequests logins, ViewedBrowser viewed) {
		this.viewed = viewed;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.accounts = accounts;
		this.logins = logins;
	}

	@ParameterType("first|second")
	public MemberBrowser browserOf(String which) {
		return "first".equals(which) ? browser : secondBrowser.browser();
	}

	@Given("Aurora has changed the password she chose, {string}, to {string}")
	public void auroraHasChangedThePasswordSheChose(String chosen, String replacement) {
		accounts.choosePasswordInPlaceOfTheIssuedOne("Aurora", chosen);
		accounts.changePassword("Aurora", chosen, replacement);
	}

	@When("the member changes the password from {string} to {string}")
	public void theMemberChangesThePassword(String current, String replacement) {
		browser.passwordChange().change(current, replacement, replacement);
	}

	@When("the member changes the password from {string} to {string} but repeats it as {string}")
	public void theMemberChangesThePasswordButRepeatsIt(String current, String replacement, String repeated) {
		browser.passwordChange().change(current, replacement, repeated);
	}

	@Given("the first member has changed the password from {string} to {string}")
	public void theFirstMemberHasChangedThePassword(String current, String replacement) {
		browser.passwordChange().change(current, replacement, replacement);
		accounts.rememberTheChange("Aurora", replacement);
	}

	@Given("the member has changed the password from {string} to {string} on the first browser")
	public void theMemberHasChangedThePasswordOnTheFirstBrowser(String current, String replacement) {
		browser.passwordChange().change(current, replacement, replacement);
		accounts.rememberTheChange("Aurora", replacement);
	}

	@Given("a member has logged in as {string} on two browsers after choosing {string} in place of the password the reset issued")
	public void aMemberHasLoggedInOnTwoBrowsersAfterChoosing(String name, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(name, password);
		browser.loginForm().logIn(name, password);
		secondBrowser.browser().loginForm().logIn(name, password);
	}

	@Given("another member has logged in as {string} with the password {string}")
	public void anotherMemberHasLoggedIn(String name, String password) {
		secondBrowser.browser().loginForm().logIn(name, password);
	}

	@When("the other member opens the overview")
	public void theOtherMemberOpensTheOverview() {
		openTheOverviewOn(secondBrowser.browser());
	}

	@When("the member opens the overview on the {browserOf} browser")
	public void theMemberOpensTheOverviewOn(MemberBrowser which) {
		openTheOverviewOn(which);
	}

	private void openTheOverviewOn(MemberBrowser which) {
		which.openHref(OVERVIEW);
		viewed.lookAt(which);
	}

	@Then("a login as {string} with the password {string} still opens the overview")
	public void aLoginStillOpensTheOverview(String name, String password) {
		Answer answer = logins.logIn(name, password);

		assertThat(answer.status()).as("the login: " + answer.body()).isEqualTo(SERVED);
	}

	@Then("the page's frame says {string}")
	public void thePagesFrameSays(String text) {
		assertThat(browser.frame().loggedInAs()).isEqualTo(text);
	}

	@Then("the page's frame offers the button {string}")
	public void thePagesFrameOffersTheButton(String label) {
		assertThat(browser.frame().buttons()).contains(label);
	}

	@Then("the page's frame offers no button {string}")
	public void thePagesFrameOffersNoButton(String label) {
		assertThat(browser.frame().buttons()).doesNotContain(label);
	}
}
