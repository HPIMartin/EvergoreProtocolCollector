package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ViewedBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleChangeSteps {
	private static final String ADMIN = "admin";
	private static final String ADMIN_PAGE = "/admin";
	private static final String ADMIN_LABEL = "Admin";
	private static final String READ_ADMIN = "read-admin.js";
	private static final int SERVED = 200;

	private final LoginRequests logins;
	private final ClientAnswer received;
	private final ServiceLog log;
	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final ViewedBrowser viewed;
	private final Accounts accounts;

	public RoleChangeSteps(LoginRequests logins, ClientAnswer received, ServiceLog log, MemberBrowser browser, SecondBrowser secondBrowser, ViewedBrowser viewed,
			Accounts accounts) {
		this.logins = logins;
		this.received = received;
		this.log = log;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.viewed = viewed;
		this.accounts = accounts;
	}

	@ParameterType("the admin|a member")
	public String whoLogsIn(String phrase) {
		return phrase;
	}

	@When("the operator asks for the list of accounts with the admin key and reads the service's log")
	public void theOperatorAsksForTheListOfAccountsWithTheAdminKey() {
		received.remember(listAskedForWithTheAdminKey());
	}

	@Then("the operator finds that list giving {string} the role {string}")
	public void theOperatorFindsThatListGivingTheRole(String name, String role) {
		assertThat(LoginRequests.roleIn(servedAnswer(), name)).isEqualTo(role);
	}

	@Then("the operator finds the list of accounts, asked for with the admin key, giving {string} the role {string}")
	public void theOperatorFindsTheListAskedForWithTheAdminKeyGivingTheRole(String name, String role) {
		received.remember(listAskedForWithTheAdminKey());

		assertThat(LoginRequests.roleIn(servedAnswer(), name)).isEqualTo(role);
	}

	@Then("the operator finds no line {string} in the service's log")
	public void theOperatorFindsNoLineInTheServicesLog(String start) {
		assertThat(log.lines()).noneMatch(logged -> logged.startsWith(start));
	}

	@Then("the admin page offers exactly {string} and {string} as the role of {string}")
	public void theAdminPageOffersExactlyAsTheRoleOf(String first, String second, String name) {
		assertThat(shownOn(viewed.current()).accountNamed(name).roleChoices()).containsExactly(first, second);
	}

	@Then("the admin page shows {string} without a choice beside {string}")
	public void theAdminPageShowsWithoutAChoiceBeside(String role, String name) {
		ShownAccount shown = shownOn(viewed.current()).accountNamed(name);

		assertThat(shown.role()).isEqualTo(role);
		assertThat(shown.roleChoices()).isEmpty();
	}

	@Then("the admin page has asked no question")
	public void theAdminPageHasAskedNoQuestion() {
		assertThat(shownOn(viewed.current()).question()).isNull();
	}

	@When("the admin chooses {string} as the role of {string} on the admin page")
	public void theAdminChoosesAsTheRoleOfOnTheAdminPage(String label, String name) {
		chooseOnTheAdminPage(label, name);
	}

	@Given("the admin has chosen {string} as the role of {string} on the admin page")
	public void theAdminHasChosenAsTheRoleOfOnTheAdminPage(String label, String name) {
		chooseOnTheAdminPage(label, name);
	}

	@Given("{word}'s account has the role {string} and the admin has chosen {string} as the role of {string} on the admin page")
	public void accountHasTheRoleAndTheAdminHasChosen(String avatar, String role, String label, String name) {
		accounts.giveTheRoleNamed(avatar, role);
		chooseOnTheAdminPage(label, name);
	}

	@Given("{word}, whose account has the role {string}, has chosen {string} as the role of {string} on the admin page")
	public void whoseAccountHasTheRoleHasChosen(String avatar, String role, String label, String name) {
		accounts.giveTheRoleNamed(avatar, role);
		MemberBrowser other = secondBrowser.browser();
		other.loginForm().logIn(avatar, accounts.chosenBy(avatar));
		other.openHref(ADMIN_PAGE);
		other.accountRoles().choose(name, label);
	}

	@Given("the admin has chosen {string} as the role of {string} on the admin page on that browser")
	public void theAdminHasChosenAsTheRoleOfOnTheAdminPageOnThatBrowser(String label, String name) {
		MemberBrowser other = secondBrowser.browser();
		if (!other.accountResets().isOpen()) {
			other.openHref(ADMIN_PAGE);
		}
		other.accountRoles().choose(name, label);
	}

	@Given("the admin has opened the admin page")
	public void theAdminHasOpenedTheAdminPage() {
		secondBrowser.browser().opening().openAsTheAdmin(ADMIN_PAGE);
	}

	@Given("the admin has chosen {string} as the role of {string} on another browser")
	public void theAdminHasChosenAsTheRoleOfOnAnotherBrowser(String label, String name) {
		browser.opening().openAsTheAdmin(ADMIN_PAGE);
		browser.accountRoles().choose(name, label);
	}

	@Given("the admin has chosen {string} as the role of {string} on the page opened before")
	public void theAdminHasChosenAsTheRoleOfOnThePageOpenedBefore(String label, String name) {
		secondBrowser.browser().accountRoles().choose(name, label);
	}

	@When("the admin opens the admin page on that browser")
	public void theAdminOpensTheAdminPageOnThatBrowser() {
		MemberBrowser other = secondBrowser.browser();
		other.openHref(ADMIN_PAGE);
		viewed.lookAt(other);
	}

	@When("{whoLogsIn} logs in as {string} with the password {string} on another browser and opens the admin page there")
	public void logsInOnAnotherBrowserAndOpensTheAdminPageThere(String who, String name, String password) {
		MemberBrowser other = secondBrowser.browser();
		other.loginForm().logIn(name, password);
		other.openHref(ADMIN_PAGE);
		viewed.lookAt(other);
	}

	@Then("the admin page on that browser reads {string}")
	public void theAdminPageOnThatBrowserReads(String message) {
		assertThat(shownOn(secondBrowser.browser()).messages()).contains(message);
	}

	@Then("the page on that browser says {string}")
	public void thePageOnThatBrowserSays(String message) {
		assertThat(secondBrowser.browser().overview().messages()).contains(message);
	}

	@Given("the admin has logged in as {string} with the admin key the service's log names since the restart")
	public void theAdminHasLoggedInWithTheAdminKeySinceTheRestart(String name) {
		browser.loginForm().logIn(name, log.adminKeyNamed());
	}

	private void chooseOnTheAdminPage(String label, String name) {
		if (browser.loginForm().wasLastUsedByTheAdmin()) {
			if (!browser.accountResets().isOpen()) {
				browser.opening().openAsTheAdmin(ADMIN_PAGE);
			}
			browser.accountRoles().choose(name, label);
		} else {
			accounts.changeRole(name, ADMIN_LABEL.equals(label) ? ADMIN : "member");
		}
	}

	private static AdminPage shownOn(MemberBrowser on) {
		return on.read(READ_ADMIN, AdminPage.class);
	}

	private Answer listAskedForWithTheAdminKey() {
		Answer loggedIn = logins.logIn(ADMIN, log.adminKeyNamed());

		return logins.listAccounts(LoginRequests.loginValueIn(loggedIn));
	}

	private Answer servedAnswer() {
		Answer listed = received.current();

		assertThat(listed.status()).as("the list of accounts: " + listed.body()).isEqualTo(SERVED);
		return listed;
	}
}
