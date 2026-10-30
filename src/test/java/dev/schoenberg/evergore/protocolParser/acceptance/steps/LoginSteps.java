package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.time.Instant;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {
	private static final String CHOOSING_A_NEW_PASSWORD = "Neues Passwort wählen";
	private static final String ONE_MINUTE_LESS_THAN = "one minute less than ";
	private static final String WRONG_PASSWORD = "Wrong-Password-1!";
	private static final String RESTING_MESSAGE = "Dein Konto ruht, bis das Protokoll wieder einen Vorgang von dir zeigt.";
	private static final String LOGIN_PAGE = "/login";
	private static final String REQUEST_LOGGED = "Client IP: ";

	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final Accounts accounts;
	private final ScenarioTime time;
	private final RunningService service;
	private final ServiceLog log;
	private Instant loggedInAt;
	private Instant firstRequest;
	private String nameEntered;

	public LoginSteps(MemberBrowser browser, SecondBrowser secondBrowser, Accounts accounts, ScenarioTime time, RunningService service, ServiceLog log) {
		this.service = service;
		this.log = log;
		this.time = time;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.accounts = accounts;
	}

	@ParameterType("\\d+ days?|one minute less than \\d+ days")
	public Duration loginAge(String age) {
		if (age.startsWith(ONE_MINUTE_LESS_THAN)) {
			return Duration.ofDays(Long.parseLong(age.substring(ONE_MINUTE_LESS_THAN.length()).split(" ")[0])).minusMinutes(1);
		}
		return Duration.ofDays(Long.parseLong(age.split(" ")[0]));
	}

	@When("the member opens the overview {loginAge} after logging in")
	public void theMemberOpensTheOverviewAfterLoggingIn(Duration age) {
		time.setTo(loggedInAt.plus(age));
		browser.openHref(OVERVIEW);
	}

	@Given("the member has opened the overview {loginAge} after logging in")
	public void theMemberHasOpenedTheOverviewAfterLoggingIn(Duration age) {
		theMemberOpensTheOverviewAfterLoggingIn(age);
	}

	@Given("the member has logged out on the first browser")
	public void theMemberHasLoggedOutOnTheFirstBrowser() {
		browser.loggingOut().logOut();
	}

	@Given("the member's browser has spent the {int} requests without a login it may send at once, the login page among them")
	public void theMembersBrowserHasSpentTheRequestsItMaySendAtOnce(int allowed) {
		service.runAsDeployed();
		firstRequest = time.now();
		int loggedBefore = requestsLogged();
		browser.openHref(LOGIN_PAGE);
		int sentByThePage = requestsLogged() - loggedBefore;

		assertThat(sentByThePage).as("the requests the login page itself took").isBetween(1, allowed);
		browser.requestsWithoutALogin().send(allowed - sentByThePage);
	}

	@When("the member logs in as {string} with the password {string} {delay} after the first request")
	public void theMemberLogsInAfterTheFirstRequest(String name, String password, Duration delay) {
		time.setTo(firstRequest.plus(delay));
		logIn(name, password);
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

	@ParameterType("reset|latest reset|first reset")
	public ResetOrder resetOrder(String phrase) {
		return ResetOrder.of(phrase);
	}

	@When("a/the member/admin logs in as {string} with the password the {resetOrder} issued")
	public void aMemberLogsInWithThePasswordTheResetIssued(String name, ResetOrder order) {
		logIn(name, order.issuedTo(accounts, name));
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

	@Given("a member has tried to log in as {string} with a wrong password")
	public void aMemberHasTriedToLogInWithAWrongPassword(String name) {
		logIn(name, WRONG_PASSWORD);
	}

	@Given("a/the member has entered the name {string} and the password the reset issued on the login page and been told the account rests")
	public void aMemberHasEnteredTheNameAndTheIssuedPasswordAndBeenToldTheAccountRests(String name) {
		logIn(name, accounts.issuedTo(name));

		assertThat(browser.overview().messages()).contains(RESTING_MESSAGE);
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

	private int requestsLogged() {
		return (int) log.lines().stream().filter(line -> line.startsWith(REQUEST_LOGGED)).count();
	}

	private void logIn(String name, String password) {
		nameEntered = name;
		loggedInAt = time.now();
		browser.loginForm().logIn(name, password);
	}
}
