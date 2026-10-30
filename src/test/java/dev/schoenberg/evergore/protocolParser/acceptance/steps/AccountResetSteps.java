package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ViewedBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountResetSteps {
	private static final String ADMIN_PAGE = "/admin";
	private static final String CANCEL = "Abbrechen";

	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final Accounts accounts;
	private final ViewedBrowser viewed;
	private final ServiceLog log;

	public AccountResetSteps(MemberBrowser browser, SecondBrowser secondBrowser, Accounts accounts, ViewedBrowser viewed, ServiceLog log) {
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.accounts = accounts;
		this.viewed = viewed;
		this.log = log;
	}

	@When("the admin asks the admin page to reset the password of {string}")
	public void theAdminAsksTheAdminPageToResetThePasswordOf(String name) {
		askOnTheAdminsBrowser(name);
	}

	@Given("the admin has asked the admin page to reset the password of {string}")
	public void theAdminHasAskedTheAdminPageToResetThePasswordOf(String name) {
		askOnTheAdminsBrowser(name);
	}

	@Given("the admin has cancelled a reset of the password of {string}")
	public void theAdminHasCancelledAResetOfThePasswordOf(String name) {
		askOnTheAdminsBrowser(name);
		browser.accountResets().answer(CANCEL);
	}

	@When("the admin answers {string}")
	public void theAdminAnswers(String buttonLabel) {
		browser.accountResets().answer(buttonLabel);
	}

	@Then("the admin page asks {string}")
	public void theAdminPageAsks(String question) {
		assertThat(browser.read("read-admin.js", AdminPage.class).question()).isEqualTo(question);
	}

	@When("the admin resets the password of {string}")
	public void theAdminResetsThePasswordOf(String name) {
		resetOnTheAdminsBrowser(name);
	}

	@Given("the admin has reset the password of {string}")
	public void theAdminHasResetThePasswordOf(String name) {
		if (browser.loginForm().wasLastUsedByTheAdmin()) {
			resetOnTheAdminsBrowser(name);
		} else {
			resetWithoutABrowser(name);
		}
	}

	@Given("the admin has reset the password of {string} and then reset it again")
	public void theAdminHasResetThePasswordOfAndThenResetItAgain(String name) {
		resetOnTheAdminsBrowser(name);
		resetOnTheAdminsBrowser(name);
	}

	@Given("the admin has logged in as {string} with the password {string} on another browser")
	public void theAdminHasLoggedInOnAnotherBrowser(String name, String password) {
		secondBrowser.browser().loginForm().logIn(name, password);
	}

	@When("the admin logs in as {string} with the admin key on another browser and opens the admin page there")
	public void theAdminLogsInWithTheAdminKeyOnAnotherBrowserAndOpensTheAdminPage(String name) {
		MemberBrowser other = secondBrowser.browser();
		other.loginForm().logIn(name, log.adminKeyNamed());
		other.openHref(ADMIN_PAGE);
		viewed.lookAt(other);
	}

	@When("the admin resets the password of {string} on that browser")
	public void theAdminResetsThePasswordOfOnThatBrowser(String name) {
		resetOnTheOtherBrowser(name);
	}

	@Given("the admin has reset the password of {string} on that browser")
	public void theAdminHasResetThePasswordOfOnThatBrowser(String name) {
		resetOnTheOtherBrowser(name);
	}

	@When("the admin reloads the admin page")
	public void theAdminReloadsTheAdminPage() {
		browser.reload();
	}

	@Given("the admin has reloaded the admin page")
	public void theAdminHasReloadedTheAdminPage() {
		browser.reload();
	}

	@When("the admin opens the overview and then the admin page")
	public void theAdminOpensTheOverviewAndThenTheAdminPage() {
		browser.openHref(OVERVIEW);
		browser.openHref(ADMIN_PAGE);
	}

	@Then("the admin page shows a password beside {string}")
	public void theAdminPageShowsAPasswordBeside(String name) {
		assertThat(issuedBeside(browser, name)).as("the password the admin page shows beside " + name).isPresent();
	}

	@Then("the admin page shows a password beside {string} and another beside {string}")
	public void theAdminPageShowsAPasswordBesideAndAnotherBeside(String first, String second) {
		Optional<String> besideTheFirst = issuedBeside(browser, first);
		Optional<String> besideTheSecond = issuedBeside(browser, second);

		assertThat(besideTheFirst).as("the password the admin page shows beside " + first).isPresent();
		assertThat(besideTheSecond).as("the password the admin page shows beside " + second).isPresent().isNotEqualTo(besideTheFirst);
	}

	@Then("the admin page on that browser shows a password beside {string}")
	public void theAdminPageOnThatBrowserShowsAPasswordBeside(String name) {
		assertThat(issuedBeside(secondBrowser.browser(), name)).as("the password the admin page on that browser shows beside " + name).isPresent();
	}

	@Then("the admin page lists {string} and {string} and shows no password")
	public void theAdminPageListsAndShowsNoPassword(String first, String second) {
		List<ShownAccount> listed = viewed.current().read("read-admin.js", AdminPage.class).accounts();

		assertThat(listed).extracting(ShownAccount::name).containsExactlyInAnyOrder(first, second);
		assertThat(listed).extracting(ShownAccount::issued).containsOnlyNulls();
	}

	@When("a/the member logs in as {string} with the password the admin page shows beside {string}")
	public void aMemberLogsInWithThePasswordTheAdminPageShowsBeside(String name, String shownBeside) {
		String shown = issuedBeside(browser, shownBeside).orElseThrow(() -> new AssertionError("The admin page shows no password beside " + shownBeside));

		browser.loginForm().logIn(name, shown);
	}

	@Then("the next page the admin opens on that browser is headed {string}")
	public void theNextPageTheAdminOpensOnThatBrowserIsHeaded(String heading) {
		secondBrowser.browser().openHref(ADMIN_PAGE);

		assertThat(secondBrowser.browser().ledger().heading()).isEqualTo(heading);
	}

	private Optional<String> issuedBeside(MemberBrowser on, String name) {
		return on
				.read("read-admin.js", AdminPage.class)
				.accounts()
				.stream()
				.filter(account -> name.equals(account.name()))
				.map(ShownAccount::issued)
				.filter(Objects::nonNull)
				.findFirst();
	}

	private void askOnTheAdminsBrowser(String name) {
		openTheAdminPageOnTheAdminsBrowser();
		browser.accountResets().ask(name);
	}

	private void openTheAdminPageOnTheAdminsBrowser() {
		if (!browser.accountResets().isOpen()) {
			browser.opening().openAsTheAdmin(ADMIN_PAGE);
		}
	}

	private void resetOnTheAdminsBrowser(String name) {
		openTheAdminPageOnTheAdminsBrowser();
		accounts.rememberIssued(name, browser.accountResets().reset(name));
	}

	private void resetWithoutABrowser(String name) {
		accounts.reset(name);
	}

	private void resetOnTheOtherBrowser(String name) {
		MemberBrowser other = secondBrowser.browser();
		if (!other.accountResets().isOpen()) {
			other.openHref(ADMIN_PAGE);
		}
		accounts.rememberIssued(name, other.accountResets().reset(name));
	}
}
