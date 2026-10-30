package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.AlteredLogins;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Asking;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.BundledFiles;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;
import dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings.GUILD_TOKEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientLoginSteps {
	private static final String OVERVIEWS_FIGURES = "/api/v1/avatars";
	private static final String ADMINS_STATUS_FIGURES = "/api/v1/admin/status";
	private static final String ACCOUNTS = "/api/v1/admin/accounts";
	private static final String RESET_OF = "the reset of the password of";
	private static final String A_CLIENT = "198.51.100.1";
	private static final String NOT_ISSUED = "Not-Issued-1!";
	private static final int SERVED = 200;
	private static final int UNAUTHORIZED = 401;
	private static final int BAD_REQUEST = 400;
	private static final int FOUND = 302;
	private static final int FORBIDDEN = 403;
	private static final int NO_CONTENT = 204;

	private final LoginRequests logins;
	private final ServiceRequests requests;
	private final Accounts accounts;
	private final ClientAnswer received;
	private final OperatorSettings settings;
	private final RunningService service;
	private final HealthReport health;
	private final ServiceLog log;
	private Answer earlierLoginAnswer;
	private String issuedLogin;
	private String copiedLogin;
	private String returnTarget;

	public ClientLoginSteps(LoginRequests logins, ServiceRequests requests, Accounts accounts, ClientAnswer received, OperatorSettings settings, RunningService service,
			HealthReport health, ServiceLog log) {
		this.log = log;
		this.health = health;
		this.logins = logins;
		this.requests = requests;
		this.accounts = accounts;
		this.received = received;
		this.settings = settings;
		this.service = service;
	}

	@ParameterType("a password the reset did not issue|no password")
	public String givenPassword(String phrase) {
		return "no password".equals(phrase) ? "" : NOT_ISSUED;
	}

	@ParameterType("the login page|a file the login page loads|the overview's figures|the movements of the bank ledger of \"Aurora\"|the admin's status figures|a file the dashboard loads"
			+ "|the change of a password with the current one|the descriptions of the service's interface for programs \\(Swagger, ReDoc and RapiDoc\\)"
			+ "|an interface address the service does not serve|the list of accounts|the reset of the password of \"[^\"]+\"")
	public List<Asking> askedFor(String phrase) {
		return switch (phrase) {
			case "the login page" -> List.of(Asking.get("/login"));
			case "a file the login page loads" -> List.of(Asking.get(BundledFiles.aFileTheLoginPageLoads()));
			case "the overview's figures" -> List.of(Asking.get(OVERVIEWS_FIGURES));
			case "the movements of the bank ledger of \"Aurora\"" -> List.of(Asking.get(OVERVIEWS_FIGURES + "/Aurora/bank"));
			case "the admin's status figures" -> List.of(Asking.get(ADMINS_STATUS_FIGURES));
			case "a file the dashboard loads" -> List.of(Asking.get(BundledFiles.aFileTheDashboardLoads()));
			case "the change of a password with the current one" -> List.of(Asking.get("/api/v1/password"));
			case "an interface address the service does not serve" -> List.of(Asking.get("/api/v1/unknown"));
			case "the list of accounts" -> List.of(Asking.get(ACCOUNTS));
			default -> phrase.startsWith(RESET_OF) ? List.of(Asking.post(LoginRequests.resetAddressOf(nameIn(phrase)))) : swaggerAddresses();
		};
	}

	private static List<Asking> swaggerAddresses() {
		return List.of(Asking.get("/swagger-ui/index.html"), Asking.get("/redoc/index.html"), Asking.get("/rapidoc/index.html"));
	}

	private static String nameIn(String phrase) {
		return phrase.substring(phrase.indexOf('"') + 1, phrase.length() - 1);
	}

	@ParameterType("altered to claim the admin's role|altered to name another account|signed by someone other than the service|that carries no signature at all")
	public UnaryOperator<String> alteredLogin(String kind) {
		return switch (kind) {
			case "altered to claim the admin's role" -> AlteredLogins::claimingTheAdminsRole;
			case "altered to name another account" -> AlteredLogins::namingAnotherAccount;
			case "signed by someone other than the service" -> AlteredLogins::signedBySomeoneOtherThanTheService;
			default -> AlteredLogins::withoutASignature;
		};
	}

	@ParameterType("by its full address|behind a doubled slash|behind a slash and a backslash")
	public String awayTarget(String how) {
		return switch (how) {
			case "by its full address" -> "https://evil.example/overview";
			case "behind a doubled slash" -> "//evil.example/overview";
			default -> "/\\evil.example/overview";
		};
	}

	@Given("a client has asked for the login page with the request to return to another site {awayTarget}")
	public void aClientHasAskedForTheLoginPageWithTheRequestToReturnToAnotherSite(String target) {
		Answer page = requests.get("/login?next=" + encoded(target), Map.of(AcceptanceClientIp.HEADER, A_CLIENT));

		assertThat(page.status()).as("the login page asked for with a return target").isEqualTo(SERVED);
		returnTarget = target;
	}

	@When("the client logs in as {string} with the password {string}")
	public void theClientLogsInAs(String name, String password) {
		received.remember(logins.logIn(name, password, returnTarget));
	}

	@Then("the operator finds the login's answer leading to the overview")
	public void theOperatorFindsTheLoginsAnswerLeadingToTheOverview() {
		assertThat(received.current().status()).as("the login: " + received.current().body()).isEqualTo(SERVED);
		assertThat(LoginRequests.nextIn(received.current())).isEqualTo("/");
	}

	@When("a client without a login asks for {askedFor}")
	public void aClientWithoutALoginAsksFor(List<Asking> askings) {
		Answer first = null;
		for (Asking asking : askings) {
			Answer answer = requests.ask(asking, Map.of(AcceptanceClientIp.HEADER, A_CLIENT));
			first = first == null || answer.status() != UNAUTHORIZED ? answer : first;
		}
		received.remember(first);
	}

	@When("a client without a login asks for the bank ledger of {string}")
	public void aClientWithoutALoginAsksForTheBankLedgerOf(String avatar) {
		received.remember(requests.get(BANK.pathOf(avatar), Map.of(AcceptanceClientIp.HEADER, A_CLIENT, "Accept", "text/html")));
	}

	@When("a client gives the name {string} and the password the reset issued and asks for the overview's figures, skipping the login page")
	public void aClientGivesTheNameAndTheIssuedPasswordInTheAddress(String name) {
		askForTheOverviewsFiguresWith(Map.of("name", name, "password", accounts.issuedTo(name)));
	}

	@When("a client asks for the overview's figures with the name {string} and the password {string} in the address")
	public void aClientAsksForTheOverviewsFiguresWithTheNameAndThePasswordInTheAddress(String name, String password) {
		askForTheOverviewsFiguresWith(Map.of("name", name, "password", password));
	}

	@When("a client asks for the overview's figures with the value of a login the service issued to {word} in the address")
	public void aClientAsksForTheOverviewsFiguresWithTheValueOfALogin(String avatar) {
		String value = LoginRequests.loginValueIn(logins.logIn(avatar, accounts.chosenBy(avatar)));
		askForTheOverviewsFiguresWith(Map.of(LoginRequests.SESSION_COOKIE, value));
	}

	@Given("the guild's old token is still set among the service's settings")
	public void theGuildsOldTokenIsStillSetAmongTheServicesSettings() {
		settings.setVariable(OperatorSettings.GUILD_TOKEN_VARIABLE, GUILD_TOKEN);

		service.restartAsDeployed().ifPresent(refused -> {
			throw refused;
		});
	}

	@When("a client asks for the overview's figures with the guild's old token in the address")
	public void aClientAsksForTheOverviewsFiguresWithTheGuildsOldToken() {
		askForTheOverviewsFiguresWith(Map.of("token", GUILD_TOKEN));
	}

	@Given("a client has tried to choose {string} as {word}'s new password, giving {givenPassword}")
	public void aClientHasTriedToChooseTheNewPasswordGiving(String newPassword, String avatar, String issued) {
		received.remember(logins.chooseNewPassword(avatar, issued, newPassword));

		assertThat(received.current().status()).as("the service's answer to the choice with a password it did not issue").isEqualTo(UNAUTHORIZED);
	}

	@Given("a client has set {string} as {word}'s new password, giving the password the reset issued, skipping the page")
	public void aClientHasSetTheNewPassword(String newPassword, String avatar) {
		received.remember(logins.chooseNewPassword(avatar, accounts.issuedTo(avatar), newPassword));

		assertThat(received.current().status()).as("the service's answer to the choice: " + received.current().body()).isEqualTo(BAD_REQUEST);
	}

	@When("a client sets {string} as {word}'s new password giving the password the reset issued, skipping the page")
	public void aClientSetsTheNewPassword(String newPassword, String avatar) {
		received.remember(logins.chooseNewPassword(avatar, accounts.issuedTo(avatar), newPassword));
	}

	@When("a client sets the password the reset issued as {word}'s new password giving the password the reset issued, skipping the page")
	public void aClientSetsTheIssuedPasswordAsTheNewPassword(String avatar) {
		String issued = accounts.issuedTo(avatar);
		received.remember(logins.chooseNewPassword(avatar, issued, issued));
	}

	@Given("a client has asked to log in as {string} with the password {string}")
	public void aClientHasAskedToLogIn(String name, String password) {
		earlierLoginAnswer = logins.logIn(name, password);
	}

	@When("a client asks to log in as {string} with the password {string}")
	public void aClientAsksToLogIn(String name, String password) {
		received.remember(logins.logIn(name, password));
	}

	@Given("a client has logged in as {string} with the password {string}")
	public void aClientHasLoggedIn(String name, String password) {
		Answer answer = logins.logIn(name, password);

		assertThat(answer.status()).as("the login: " + answer.body()).isEqualTo(SERVED);
		issuedLogin = LoginRequests.loginValueIn(answer);
	}

	@Given("a client has logged in as {string} with the admin key the service's log names")
	public void aClientHasLoggedInWithTheAdminKey(String name) {
		aClientHasLoggedIn(name, log.adminKeyNamed());
	}

	@Given("a client has logged in as {string} after choosing {string} in place of the password the reset issued")
	public void aClientHasLoggedInAfterChoosing(String name, String password) {
		accounts.choosePasswordInPlaceOfTheIssuedOne(name, password);
		aClientHasLoggedIn(name, password);
	}

	@When("another client asks for {askedFor} with a login {alteredLogin}")
	public void anotherClientAsksForWithAnAlteredLogin(List<Asking> askings, UnaryOperator<String> alteration) {
		received.remember(logins.asking(askings.getFirst(), alteration.apply(issuedLogin)));
	}

	@ParameterType("the current password \"[^\"]*\"|no current password|\"[^\"]*\"|the admin key")
	public Optional<String> givenCurrent(String phrase) {
		if ("no current password".equals(phrase)) {
			return Optional.empty();
		}
		if ("the admin key".equals(phrase)) {
			return Optional.of(log.adminKeyNamed());
		}
		return Optional.of(phrase.substring(phrase.indexOf('"') + 1, phrase.length() - 1));
	}

	@When("a client sets {string} as {word}'s new password with her login, giving {givenCurrent}, skipping the page")
	public void aClientSetsTheNewPasswordWithHerLogin(String newPassword, String avatar, Optional<String> current) {
		received.remember(logins.changePassword(issuedLogin, current, newPassword));
	}

	@When("a client sets {string} as the new password with its login, giving {givenCurrent}, skipping the page")
	public void aClientSetsTheNewPasswordWithItsLogin(String newPassword, Optional<String> current) {
		received.remember(logins.changePassword(issuedLogin, current, newPassword));
	}

	@Then("the operator finds the request refused for a wrong current password")
	public void theOperatorFindsTheRequestRefusedForAWrongCurrentPassword() {
		assertThat(received.current().status()).isEqualTo(BAD_REQUEST);
		assertThat(received.current().body()).isEqualTo("{\"refusal\":\"wrong-current-password\"}");
	}

	@Then("the operator finds the request refused for lacking an account")
	public void theOperatorFindsTheRequestRefusedForLackingAnAccount() {
		assertThat(received.current().status()).isEqualTo(FORBIDDEN);
		assertThat(received.current().body()).isEqualTo("{\"refusal\":\"no-account\"}");
	}

	@Then("the operator finds a login as {string} with the password {string} opening the overview's figures")
	public void theOperatorFindsALoginOpeningTheOverviewsFigures(String name, String password) {
		theOperatorFindsANewLoginOpeningTheOverviewsFigures(name, password);
	}

	@Then("the operator finds a login as {string} with the password {string} answered as a wrong password")
	public void theOperatorFindsALoginAnsweredAsAWrongPassword(String name, String password) {
		Answer answer = logins.logIn(name, password);

		assertThat(answer.status()).isEqualTo(UNAUTHORIZED);
		assertThat(answer.body()).isEqualTo("{\"refusal\":\"wrong-name-or-password\"}");
	}

	@When("a client gives the name {string} and the password the reset issued and chooses {string} as the new password, skipping the login page")
	public void aClientChoosesTheNewPasswordSkippingTheLoginPage(String name, String newPassword) {
		Answer chosen = logins.chooseNewPassword(name, accounts.issuedTo(name), newPassword);
		Answer figures = chosen.header("Set-Cookie").isPresent()
				? logins.asking(OVERVIEWS_FIGURES, LoginRequests.loginValueIn(chosen))
				: requests.get(OVERVIEWS_FIGURES, Map.of(AcceptanceClientIp.HEADER, A_CLIENT));

		received.remember(figures);
	}

	@When("a client logs in as {string} with the password {string}")
	public void aClientLogsInAs(String name, String password) {
		received.remember(logins.logIn(name, password));
	}

	@Given("another client has taken a copy of that login")
	public void anotherClientHasTakenACopyOfThatLogin() {
		copiedLogin = issuedLogin;
	}

	@Given("the first client has logged out")
	public void theFirstClientHasLoggedOut() {
		Answer answer = logins.logOut(issuedLogin);

		assertThat(answer.status()).as("the logout: " + answer.body()).isEqualTo(NO_CONTENT);
	}

	@When("the client logs out")
	public void theClientLogsOut() {
		received.remember(logins.logOut(issuedLogin));
	}

	@When("the other client asks for the overview's figures with the copied login")
	public void theOtherClientAsksForTheOverviewsFiguresWithTheCopiedLogin() {
		received.remember(logins.asking(OVERVIEWS_FIGURES, copiedLogin));
	}

	@Then("the operator finds a new login as {string} with the password {string} opening the overview's figures")
	public void theOperatorFindsANewLoginOpeningTheOverviewsFigures(String name, String password) {
		String login = LoginRequests.loginValueIn(logins.logIn(name, password));

		Answer figures = logins.asking(OVERVIEWS_FIGURES, login);

		assertThat(figures.status()).isEqualTo(SERVED);
	}

	@Then("the operator finds the login kept for 7 days counted from its arrival in the browser, whatever date the service's clock reads")
	public void theOperatorFindsTheLoginKeptForSevenDays() {
		String cookie = setCookie();

		assertThat(cookie).contains("Max-Age=" + Login.LIFETIME.toSeconds()).doesNotContainIgnoringCase("Expires");
	}

	@Then("the operator finds the login out of reach of the page's scripts")
	public void theOperatorFindsTheLoginOutOfReachOfTheScripts() {
		assertThat(setCookie()).contains("HttpOnly");
	}

	@Then("the operator finds the login not sent along with what another site's page sends on its own")
	public void theOperatorFindsTheLoginNotSentWithWhatAnotherSiteSends() {
		assertThat(setCookie()).contains("SameSite=Lax");
	}

	@Then("the operator finds the login not marked for HTTPS only, since the service still runs over plain HTTP")
	public void theOperatorFindsTheLoginNotMarkedForHttpsOnly() {
		assertThat(setCookie()).doesNotContainIgnoringCase("Secure");
	}

	@Then("the operator finds the login cleared from the client's browser")
	public void theOperatorFindsTheLoginClearedFromTheClientsBrowser() {
		assertThat(setCookie()).startsWith(LoginRequests.SESSION_COOKIE + "=;").contains("Max-Age=0");
	}

	@When("the client asks for {askedFor} with its login")
	public void theClientAsksForWithItsLogin(List<Asking> askings) {
		received.remember(logins.asking(askings.getFirst(), issuedLogin));
	}

	@When("the client asks the service for its health with its login")
	public void theClientAsksTheServiceForItsHealthWithItsLogin() {
		health.askWithTheLogin(issuedLogin);
	}

	@Then("the operator finds the request refused for lacking the admin's role")
	public void theOperatorFindsTheRequestRefusedForLackingTheAdminsRole() {
		assertThat(received.current().status()).isEqualTo(FORBIDDEN);
	}

	@Then("the operator finds the login the service issued to Aurora opening the overview's figures")
	public void theOperatorFindsTheLoginTheServiceIssuedOpeningTheOverviewsFigures() {
		Answer figures = logins.asking(OVERVIEWS_FIGURES, issuedLogin);

		assertThat(figures.status()).isEqualTo(SERVED);
	}

	@Then("the operator finds the same status and message as for a login as {string} with a wrong password")
	public void theOperatorFindsTheSameStatusAndMessage(String name) {
		assertThat(received.current().status()).as("the status of a login as " + name + " with a wrong password").isEqualTo(earlierLoginAnswer.status());
		assertThat(received.current().body()).isEqualTo(earlierLoginAnswer.body());
	}

	@Then("the operator finds the client redirected to the login page, with the request to return to the bank ledger of {string}")
	public void theOperatorFindsTheClientRedirectedToTheLoginPage(String avatar) {
		assertThat(received.current().status()).isEqualTo(FOUND);
		assertThat(received.current().header("Location")).contains("/login?next=" + encoded(BANK.pathOf(avatar)));
	}

	@Then("the operator finds the request refused for lacking a login")
	public void theOperatorFindsTheRequestRefusedForLackingALogin() {
		assertThat(received.current().status()).isEqualTo(UNAUTHORIZED);
	}

	@Then("the operator finds the request refused for breaking the password rule")
	public void theOperatorFindsTheRequestRefusedForBreakingThePasswordRule() {
		assertThat(received.current().status()).isEqualTo(BAD_REQUEST);
	}

	private String setCookie() {
		return received.current().header("Set-Cookie").orElseThrow(() -> new AssertionError("The service set no cookie, it answered " + received.current().status()));
	}

	private void askForTheOverviewsFiguresWith(Map<String, String> parameters) {
		Map<String, String> inTheAddress = new LinkedHashMap<>(parameters);
		String query = inTheAddress.entrySet().stream().map(parameter -> parameter.getKey() + "=" + encoded(parameter.getValue())).collect(joining("&"));

		received.remember(requests.get(OVERVIEWS_FIGURES + "?" + query, Map.of(AcceptanceClientIp.HEADER, A_CLIENT)));
	}

	private static String encoded(String text) {
		return URLEncoder.encode(text, UTF_8);
	}
}
