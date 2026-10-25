package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientLoginSteps {
	private static final String OVERVIEWS_FIGURES = "/api/v1/avatars";
	private static final String A_CLIENT = "198.51.100.1";
	private static final String NOT_ISSUED = "Not-Issued-1!";
	private static final int UNAUTHORIZED = 401;
	private static final int BAD_REQUEST = 400;
	private static final int FOUND = 302;

	private final LoginRequests logins;
	private final ServiceRequests requests;
	private final Accounts accounts;
	private Answer answer;

	public ClientLoginSteps(LoginRequests logins, ServiceRequests requests, Accounts accounts) {
		this.logins = logins;
		this.requests = requests;
		this.accounts = accounts;
	}

	@ParameterType("a password the reset did not issue|no password")
	public String givenPassword(String phrase) {
		return "no password".equals(phrase) ? "" : NOT_ISSUED;
	}

	@When("a client gives the name {string} and the password the reset issued and asks for the overview's figures, skipping the login page")
	public void aClientGivesTheNameAndTheIssuedPasswordInTheAddress(String name) {
		Map<String, String> credentials = new LinkedHashMap<>();
		credentials.put("name", name);
		credentials.put("password", accounts.issuedTo(name));
		String query = credentials.entrySet().stream().map(parameter -> parameter.getKey() + "=" + encoded(parameter.getValue())).collect(joining("&"));

		answer = requests.get(OVERVIEWS_FIGURES + "?" + query, Map.of(AcceptanceClientIp.HEADER, A_CLIENT));
	}

	@Given("a client has tried to choose {string} as {word}'s new password, giving {givenPassword}")
	public void aClientHasTriedToChooseTheNewPasswordGiving(String newPassword, String avatar, String issued) {
		answer = logins.chooseNewPassword(avatar, issued, newPassword);

		assertThat(answer.status()).as("the service's answer to the choice with a password it did not issue").isEqualTo(UNAUTHORIZED);
	}

	@Given("a client has set {string} as {word}'s new password, giving the password the reset issued, skipping the page")
	public void aClientHasSetTheNewPassword(String newPassword, String avatar) {
		answer = logins.chooseNewPassword(avatar, accounts.issuedTo(avatar), newPassword);

		assertThat(answer.status()).as("the service's answer to the choice: " + answer.body()).isEqualTo(BAD_REQUEST);
	}

	@When("a client sets {string} as {word}'s new password giving the password the reset issued, skipping the page")
	public void aClientSetsTheNewPassword(String newPassword, String avatar) {
		answer = logins.chooseNewPassword(avatar, accounts.issuedTo(avatar), newPassword);
	}

	@When("a client sets the password the reset issued as {word}'s new password giving the password the reset issued, skipping the page")
	public void aClientSetsTheIssuedPasswordAsTheNewPassword(String avatar) {
		String issued = accounts.issuedTo(avatar);
		answer = logins.chooseNewPassword(avatar, issued, issued);
	}

	@When("a client without a login asks for the bank ledger of {string}")
	public void aClientWithoutALoginAsksForTheBankLedgerOf(String avatar) {
		answer = requests.get(BANK.pathOf(avatar), Map.of(AcceptanceClientIp.HEADER, A_CLIENT, "Accept", "text/html"));
	}

	@Then("the operator finds the client redirected to the login page, with the request to return to the bank ledger of {string}")
	public void theOperatorFindsTheClientRedirectedToTheLoginPage(String avatar) {
		assertThat(answer.status()).isEqualTo(FOUND);
		assertThat(answer.header("Location")).contains("/login?next=" + encoded(BANK.pathOf(avatar)));
	}

	@Then("the operator finds the request refused for lacking a login")
	public void theOperatorFindsTheRequestRefusedForLackingALogin() {
		assertThat(answer.status()).isEqualTo(UNAUTHORIZED);
	}

	@Then("the operator finds the request refused for breaking the password rule")
	public void theOperatorFindsTheRequestRefusedForBreakingThePasswordRule() {
		assertThat(answer.status()).isEqualTo(BAD_REQUEST);
	}

	private static String encoded(String text) {
		return URLEncoder.encode(text, UTF_8);
	}
}
