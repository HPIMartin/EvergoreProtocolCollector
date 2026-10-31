package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Arrays;
import java.util.List;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownFinding;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownFindingList;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownLink;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ViewedBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminSteps {
	private static final String NAME_SEPARATOR = ", ";
	private static final String USAGE_SETTLED = "(() => { const title = document.querySelector('[data-testid=usage-title]'); if (title === null) { return true }"
			+ " const section = title.parentElement; if (section.querySelector('[data-testid=usage]') !== null) { return true }"
			+ " const panel = section.querySelector('[data-testid=status-panel]'); return panel !== null && panel.textContent.trim() !== 'Wird geladen…' })()";

	private final AdminsBrowser admins;
	private final SecondBrowser secondBrowser;
	private final ViewedBrowser viewed;

	public AdminSteps(AdminsBrowser admins, SecondBrowser secondBrowser, ViewedBrowser viewed) {
		this.admins = admins;
		this.secondBrowser = secondBrowser;
		this.viewed = viewed;
	}

	@When("the admin opens the admin page")
	public void theAdminOpensTheAdminPage() {
		admins.openTheAdminPage();
	}

	@When("the admin follows the name in the entry {string} under {string} on the admin page")
	public void theAdminFollowsTheNameInTheEntryUnderOnTheAdminPage(String entry, String list) {
		MemberBrowser opened = secondBrowser.browser();

		opened.followTheLinkIn(list, entry);
		viewed.lookAt(opened);
	}

	@Then("in the entry {string} under {string} only {string} leads anywhere")
	public void inTheEntryUnderOnlyLeadsAnywhere(String entry, String list, String member) {
		ShownFinding shown = findingNamed(list, entry);

		assertThat(shown.links()).extracting(ShownLink::text).containsExactly(member);
		assertThat(shown.links()).extracting(ShownLink::href).doesNotContainNull();
	}

	@Then("{string} stands under {string} on the admin page and leads nowhere")
	public void standsUnderOnTheAdminPageAndLeadsNowhere(String name, String list) {
		ShownFindingList shown = findingList(list);

		assertThat(shown.text()).isEqualTo(list + ": " + name);
		assertThat(shown.links()).isEmpty();
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
		assertThat(adminPage().accountNamed(name).lastLogin()).isEqualTo(lastLogin);
	}

	@Then("the admin page gives {string} as the role of {string}")
	public void theAdminPageGivesAsTheRoleOf(String role, String name) {
		AdminPage viewedPage = viewed.current().read("read-admin.js", AdminPage.class);

		assertThat(viewedPage.accountNamed(name).role()).isEqualTo(role);
	}

	private ShownFinding findingNamed(String list, String entry) {
		return findingList(list)
				.entries()
				.stream()
				.filter(shown -> entry.equals(shown.text()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The list " + list + " has no entry " + entry));
	}

	private ShownFindingList findingList(String heading) {
		return adminPage()
				.findings()
				.stream()
				.filter(shown -> heading.equals(shown.heading()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The admin page shows no list headed " + heading));
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
