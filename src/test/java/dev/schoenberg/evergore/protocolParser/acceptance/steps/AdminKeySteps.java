package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Locale;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class AdminKeySteps {
	private static final String ADMINS_STATUS_FIGURES = "/api/v1/admin/status";
	private static final String RANDOM_KEY_OF_43_CHARACTERS = "[A-Za-z0-9_-]{43}";
	private static final int SERVED = 200;

	private final ServiceLog log;
	private final LoginRequests logins;
	private final MemberBrowser browser;
	private String keyNamedBefore;

	public AdminKeySteps(ServiceLog log, LoginRequests logins, MemberBrowser browser) {
		this.log = log;
		this.logins = logins;
		this.browser = browser;
	}

	@Then("the operator finds the line {string} in the service's log")
	public void theOperatorFindsTheLineInTheServicesLog(String line) {
		assertThat(log.lines()).contains(line);
	}

	@Then("the operator finds the line {string} followed by a random key of 43 characters in the service's log")
	public void theOperatorFindsTheLineFollowedByARandomKey(String line) {
		assertThat(log.lines()).anyMatch(logged -> logged.startsWith(line) && logged.substring(line.length()).matches(RANDOM_KEY_OF_43_CHARACTERS));
	}

	@Given("the service's log has named its admin key")
	public void theServicesLogHasNamedItsAdminKey() {
		keyNamedBefore = log.adminKeyNamed();
	}

	@Given("the admin has logged in as {string} with the admin key the service's log names")
	public void theAdminHasLoggedInWithTheAdminKey(String name) {
		browser.loginForm().logIn(name, log.adminKeyNamed());
	}

	@When("the admin logs in as {string} with the admin key the service's log names")
	public void theAdminLogsInWithTheAdminKey(String name) {
		browser.loginForm().logIn(name, log.adminKeyNamed());
	}

	@When("the admin logs in as {string} with the admin key the service's log names, written in other capitals")
	public void theAdminLogsInWithTheAdminKeyWrittenInOtherCapitals(String name) {
		String key = log.adminKeyNamed();
		String swapped = key.chars().mapToObj(character -> String.valueOf(swapCase((char) character))).reduce("", String::concat);

		assertThat(swapped).as("the key with its capitals swapped").isNotEqualTo(key);
		browser.loginForm().logIn(name, swapped);
	}

	@When("the admin logs in as {string} with the admin key named before the restart")
	public void theAdminLogsInWithTheAdminKeyNamedBeforeTheRestart(String name) {
		browser.loginForm().logIn(name, keyNamedBefore);
	}

	@When("the admin opens the overview")
	public void theAdminOpensTheOverview() {
		browser.opening().open(OVERVIEW);
	}

	@Then("the operator finds a login as {string} with the admin key the service's log names opening the admin's status figures")
	public void theOperatorFindsALoginWithTheAdminKeyOpeningTheAdminsStatusFigures(String name) {
		Answer loggedIn = logins.logIn(name, log.adminKeyNamed());

		Answer figures = logins.asking(ADMINS_STATUS_FIGURES, LoginRequests.loginValueIn(loggedIn));

		assertThat(figures.status()).as("the admin's status figures: " + figures.body()).isEqualTo(SERVED);
	}

	private static char swapCase(char character) {
		return Character.isUpperCase(character) ? String.valueOf(character).toLowerCase(Locale.ROOT).charAt(0) : String.valueOf(character).toUpperCase(Locale.ROOT).charAt(0);
	}
}
