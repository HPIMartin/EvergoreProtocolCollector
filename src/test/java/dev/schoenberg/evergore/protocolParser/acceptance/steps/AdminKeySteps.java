package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;

import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminKeySteps {
	private static final String ANNOUNCEMENT = "Console admin key, valid until the next restart: ";
	private static final String ADMINS_STATUS_FIGURES = "/api/v1/admin/status";
	private static final int SERVED = 200;

	private final ServiceLog log;
	private final LoginRequests logins;

	public AdminKeySteps(ServiceLog log, LoginRequests logins) {
		this.log = log;
		this.logins = logins;
	}

	@Then("the operator finds the line {string} in the service's log")
	public void theOperatorFindsTheLineInTheServicesLog(String line) {
		assertThat(log.lines()).contains(line);
	}

	@Then("the operator finds a login as {string} with the admin key the service's log names opening the admin's status figures")
	public void theOperatorFindsALoginWithTheAdminKeyOpeningTheAdminsStatusFigures(String name) {
		Answer loggedIn = logins.logIn(name, adminKeyNamedInTheLog());

		Answer figures = logins.asking(ADMINS_STATUS_FIGURES, LoginRequests.loginValueIn(loggedIn));

		assertThat(figures.status()).as("the admin's status figures: " + figures.body()).isEqualTo(SERVED);
	}

	private String adminKeyNamedInTheLog() {
		List<String> announcements = log.lines().stream().filter(line -> line.startsWith(ANNOUNCEMENT)).toList();

		assertThat(announcements).as("the admin key the service's log names").isNotEmpty();
		return announcements.getLast().substring(ANNOUNCEMENT.length());
	}
}
