package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Arrays;
import java.util.List;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminSteps {
	private static final String ADMIN = "/admin";
	private static final String NAME_SEPARATOR = ", ";

	private final MemberBrowser browser;

	public AdminSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("the admin opens the admin page")
	public void theAdminOpensTheAdminPage() {
		browser.opening().openAsTheAdmin(ADMIN);
	}

	@Then("the admin page reads {string}")
	public void theAdminPageReads(String message) {
		assertThat(adminPage().messages()).contains(message);
	}

	@Then("the admin page lists exactly these accounts:")
	public void theAdminPageListsExactlyTheseAccounts(DataTable expected) {
		List<List<String>> shown = adminPage().accounts().stream().map(ShownAccount::cells).toList();

		List<List<String>> listed = expected.asLists().stream().skip(1).map(row -> row.stream().map(cell -> cell == null ? "" : cell).toList()).toList();

		assertThat(shown).containsExactlyInAnyOrderElementsOf(listed);
	}

	@Then("the admin page lists exactly the accounts {string}")
	public void theAdminPageListsExactlyTheAccounts(String names) {
		assertThat(namesShown()).containsExactlyInAnyOrderElementsOf(Arrays.asList(names.split(NAME_SEPARATOR)));
	}

	@Then("the admin page lists the accounts in this order: {quotedNames}")
	public void theAdminPageListsTheAccountsInThisOrder(List<String> names) {
		assertThat(namesShown()).isEqualTo(names);
	}

	@Then("the admin page gives {string} as the latest login of {string}")
	public void theAdminPageGivesAsTheLatestLoginOf(String lastLogin, String name) {
		assertThat(accountNamed(name).lastLogin()).isEqualTo(lastLogin);
	}

	private ShownAccount accountNamed(String name) {
		return adminPage()
				.accounts()
				.stream()
				.filter(account -> name.equals(account.name()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The admin page lists no account of " + name));
	}

	private List<String> namesShown() {
		return adminPage().accounts().stream().map(ShownAccount::name).toList();
	}

	private AdminPage adminPage() {
		return browser.read("read-admin.js", AdminPage.class);
	}
}
