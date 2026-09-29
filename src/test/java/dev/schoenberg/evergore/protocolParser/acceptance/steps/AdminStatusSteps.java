package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminStatusSteps {
	private static final String ADMIN = "/admin";

	private final MemberBrowser browser;

	public AdminStatusSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("the admin opens the admin page without the guild's token")
	public void theAdminOpensTheAdminPageWithoutTheToken() {
		browser.openWithoutToken(ADMIN);
	}

	@Then("the admin page reads:")
	public void theAdminPageReadsLines(DataTable lines) {
		AdminPage page = adminPage();

		assertThat(page.messages()).containsSubsequence(lines.asList());
	}

	AdminPage adminPage() {
		return browser.read("read-admin.js", AdminPage.class);
	}
}
