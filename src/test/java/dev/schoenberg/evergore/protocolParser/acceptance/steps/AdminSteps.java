package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminSteps {
	private static final String ADMIN = "/admin";

	private final MemberBrowser browser;

	public AdminSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("the admin opens the admin page")
	public void theAdminOpensTheAdminPage() {
		browser.open(ADMIN);
	}

	@Then("the admin page reads {string}")
	public void theAdminPageReads(String message) {
		assertThat(adminPage().messages()).contains(message);
	}

	private AdminPage adminPage() {
		return browser.read("read-admin.js", AdminPage.class);
	}
}
