package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountResetSteps {
	private static final String ADMIN_PAGE = "/admin";
	private static final String CANCEL = "Abbrechen";

	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final Accounts accounts;

	public AccountResetSteps(MemberBrowser browser, SecondBrowser secondBrowser, Accounts accounts) {
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.accounts = accounts;
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

	@Given("the admin has reset the password of {string} and then reset it again")
	public void theAdminHasResetThePasswordOfAndThenResetItAgain(String name) {
		resetOnTheAdminsBrowser(name);
		resetOnTheAdminsBrowser(name);
	}

	@Given("the admin has logged in as {string} with the password {string} on another browser")
	public void theAdminHasLoggedInOnAnotherBrowser(String name, String password) {
		secondBrowser.browser().loginForm().logIn(name, password);
	}

	@When("the admin resets the password of {string} on that browser")
	public void theAdminResetsThePasswordOfOnThatBrowser(String name) {
		resetOnTheOtherBrowser(name);
	}

	@Given("the admin has reset the password of {string} on that browser")
	public void theAdminHasResetThePasswordOfOnThatBrowser(String name) {
		resetOnTheOtherBrowser(name);
	}

	@Then("the admin page on that browser shows a password beside {string}")
	public void theAdminPageOnThatBrowserShowsAPasswordBeside(String name) {
		AdminPage page = secondBrowser.browser().read("read-admin.js", AdminPage.class);

		assertThat(page.accounts()).filteredOn(account -> name.equals(account.name())).extracting(ShownAccount::issued).singleElement().asString().isNotBlank();
	}

	@Then("the next page the admin opens on that browser is headed {string}")
	public void theNextPageTheAdminOpensOnThatBrowserIsHeaded(String heading) {
		secondBrowser.browser().openHref(ADMIN_PAGE);

		assertThat(secondBrowser.browser().ledger().heading()).isEqualTo(heading);
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

	private void resetOnTheOtherBrowser(String name) {
		MemberBrowser other = secondBrowser.browser();
		if (!other.accountResets().isOpen()) {
			other.openHref(ADMIN_PAGE);
		}
		accounts.rememberIssued(name, other.accountResets().reset(name));
	}
}
