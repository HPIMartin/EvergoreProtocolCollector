package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminStatusSteps {
	private final MemberBrowser browser;

	public AdminStatusSteps(MemberBrowser browser) {
		this.browser = browser;
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
