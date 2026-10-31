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
	private static final String NAME_SEPARATOR = ", ";
	private static final String USAGE_SETTLED = "(() => { const title = document.querySelector('[data-testid=usage-title]'); if (title === null) { return true }"
			+ " const section = title.parentElement; if (section.querySelector('[data-testid=usage]') !== null) { return true }"
			+ " const panel = section.querySelector('[data-testid=status-panel]'); return panel !== null && panel.textContent.trim() !== 'Wird geladen…' })()";

	private final AdminsBrowser admins;

	public AdminSteps(AdminsBrowser admins) {
		this.admins = admins;
	}

	@When("the admin opens the admin page")
	public void theAdminOpensTheAdminPage() {
		admins.openTheAdminPage();
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

	@Then("the admin page gives {string} as the role of {string}")
	public void theAdminPageGivesAsTheRoleOf(String role, String name) {
		assertThat(accountNamed(name).role()).isEqualTo(role);
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
		MemberBrowser browser = admins.current();
		browser.awaitUntil(USAGE_SETTLED);
		return browser.read("read-admin.js", AdminPage.class);
	}
}
