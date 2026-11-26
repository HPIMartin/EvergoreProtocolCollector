package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ShownAccount;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.UsageTable;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.acceptance.world.UsageActors;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.EXPLANATION;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

public class UsageSteps {
	private static final String USAGE_FIGURES = "/api/v1/admin/usage";
	private static final String A_PAGE_THE_DASHBOARD_DOES_NOT_HAVE = "/neuigkeiten";
	private static final String LAST_30_DAYS = "Letzte 30 Tage";
	private static final String ALL_THE_TIME = "Alles";
	private static final String GUILD = "Gilde";
	private static final String ONE_MINUTE_MORE_THAN = "one minute more than ";
	private static final String NEVER = "noch nie";
	private static final DateTimeFormatter BERLIN_MINUTE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(APP_ZONE);
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Map<String, String> FIGURES = Map.of("Anmeldungen", "logins", "Seite geladen", "pageLoads", "Abgemeldet", "logouts", "Tage genutzt", "daysUsed");

	private final AdminsBrowser admins;
	private final SecondBrowser secondBrowser;
	private final UsageActors actors;
	private final ScenarioTime time;
	private final LoginRequests logins;
	private final ServiceLog log;
	private final RunningService service;

	public UsageSteps(AdminsBrowser admins, SecondBrowser secondBrowser, UsageActors actors, ScenarioTime time, LoginRequests logins, ServiceLog log, RunningService service) {
		this.service = service;
		this.admins = admins;
		this.secondBrowser = secondBrowser;
		this.actors = actors;
		this.time = time;
		this.logins = logins;
		this.log = log;
	}

	@ParameterType("\\d+ days|one minute more than \\d+ days")
	public Duration usageAge(String age) {
		if (age.startsWith(ONE_MINUTE_MORE_THAN)) {
			return Duration.ofDays(Long.parseLong(age.substring(ONE_MINUTE_MORE_THAN.length()).split(" ")[0])).plusMinutes(1);
		}
		return Duration.ofDays(Long.parseLong(age.split(" ")[0]));
	}

	@ParameterType("over the last 30 days|over all the time recorded")
	public String usageWindow(String phrase) {
		return phrase.endsWith("30 days") ? LAST_30_DAYS : ALL_THE_TIME;
	}

	@Given("{word} has opened a bookmark of the overview {int} day(s) after logging in")
	public void hasOpenedABookmarkOfTheOverviewAfterLoggingIn(String name, int days) {
		time.setTo(actors.loggedInAt(name).plus(Duration.ofDays(days)));
		actors.browserOf(name).openHref(OVERVIEW);
	}

	@Given("{word} has logged out")
	public void hasLoggedOut(String name) {
		actors.browserOf(name).loggingOut().logOut();
	}

	@Given("the member has logged out {int} day(s) after logging in")
	public void theMemberHasLoggedOutAfterLoggingIn(int days) {
		String member = actors.lastMember();
		time.setTo(actors.loggedInAt(member).plus(Duration.ofDays(days)));
		actors.browserOf(member).loggingOut().logOut();
	}

	@Given("the member has reloaded the overview")
	public void theMemberHasReloadedTheOverview() {
		actors.browserOf(actors.lastMember()).reload();
	}

	@Given("the member has opened a link from the guild's chat to a page the dashboard does not have")
	public void theMemberHasOpenedALinkFromTheGuildsChat() {
		actors.browserOf(actors.lastMember()).followALinkFromAnotherSite(A_PAGE_THE_DASHBOARD_DOES_NOT_HAVE);
	}

	@Given("the member has followed the name of {string} in the overview")
	public void theMemberHasFollowedTheNameInTheOverview(String name) {
		actors.browserOf(actors.lastMember()).follow(name);
	}

	@Given("a member has logged in as {string} with the password {string} {int} days after the admin logged in")
	public void aMemberHasLoggedInAfterTheAdminLoggedIn(String name, String password, int days) {
		time.setTo(actors.adminLoggedInAt().plus(Duration.ofDays(days)));
		MemberBrowser own = secondBrowser.browser();
		own.loginForm().logIn(name, password);
		actors.memberLoggedIn(name, time.now(), own);
	}

	@Given("the admin has opened the explanation page")
	public void theAdminHasOpenedTheExplanationPage() {
		adminsBrowser().openHref(EXPLANATION);
	}

	@Given("the admin has opened a bookmark of the overview {int} days after logging in")
	public void theAdminHasOpenedABookmarkOfTheOverviewAfterLoggingIn(int days) {
		time.setTo(actors.adminLoggedInAt().plus(Duration.ofDays(days)));
		adminsBrowser().openHref(OVERVIEW);
	}

	@Given("the admin has opened the admin page {usageAge} after {word} logged in")
	public void theAdminHasOpenedTheAdminPageAfterLoggedIn(Duration age, String name) {
		theAdminOpensTheAdminPageAfterLoggedIn(age, name);
	}

	@When("the admin opens the admin page {usageAge} after {word} logged in")
	public void theAdminOpensTheAdminPageAfterLoggedIn(Duration age, String name) {
		time.setTo(actors.loggedInAt(name).plus(age));
		openTheAdminPage();
	}

	@Given("the admin has switched the usage to {string}")
	public void theAdminHasSwitchedTheUsageTo(String window) {
		theAdminSwitchesTheUsageTo(window);
	}

	@When("the admin switches the usage to {string}")
	public void theAdminSwitchesTheUsageTo(String window) {
		awaitTheUsage();
		adminsBrowser().choose(window);
	}

	@Then("the admin page shows the usage over {string}")
	public void theAdminPageShowsTheUsageOver(String window) {
		assertThat(usage().window()).isEqualTo(window);
	}

	@Then("the admin page gives {string} as {word}'s {string} {usageWindow}")
	public void theAdminPageGivesAsTheAccountsFigureOver(String expected, String name, String column, String window) {
		UsageTable usage = usageOver(window);

		assertThat(usage.cellOf(usage.rowOf(UsageTable.ACCOUNT, name), column)).isEqualTo(expected);
	}

	@Then("the admin page gives {string} as {word}'s {string}")
	public void theAdminPageGivesAsTheAccountsFigure(String expected, String name, String column) {
		UsageTable usage = usage();

		assertThat(usage.cellOf(usage.rowOf(UsageTable.ACCOUNT, name), column)).isEqualTo(expected);
	}

	@Then("the admin page gives {string} as the guild's {string} {usageWindow}")
	public void theAdminPageGivesAsTheGuildsFigureOver(String expected, String column, String window) {
		UsageTable usage = usageOver(window);

		assertThat(usage.cellOf(usage.rowOf(UsageTable.GUILD, GUILD), column)).isEqualTo(expected);
	}

	@Then("the admin page gives {string} as admin's {string}, in the row below the guild's")
	public void theAdminPageGivesAsTheAdminKeysFigure(String expected, String column) {
		assertTheAdminKeysFigure(usage(), expected, column);
	}

	@Then("the admin page gives {string} as admin's {string} {usageWindow}, in the row below the guild's")
	public void theAdminPageGivesAsTheAdminKeysFigureOver(String expected, String column, String window) {
		assertTheAdminKeysFigure(usageOver(window), expected, column);
	}

	@Then("the admin page shows exactly these rows of the members and the guild {usageWindow}, in this order, in these columns:")
	public void theAdminPageShowsExactlyTheseRows(String window, DataTable expected) {
		UsageTable usage = usageOver(window);
		List<String> columns = expected.row(0);
		List<List<String>> shown = usage.rows().stream().filter(row -> !row.kind().equals(UsageTable.ADMIN_KEY)).map(row -> usage.columnsOf(row.cells(), columns)).toList();

		assertThat(shown).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("the admin page shows these rows of {word} and the guild, among the others, {usageWindow}, in this order, in these columns:")
	public void theAdminPageShowsTheseRowsAmongTheOthers(String name, String window, DataTable expected) {
		UsageTable usage = usageOver(window);
		List<String> columns = expected.row(0);
		List<List<String>> shown = List.of(usage.columnsOf(usage.rowOf(UsageTable.ACCOUNT, name), columns), usage.columnsOf(usage.rowOf(UsageTable.GUILD, GUILD), columns));

		assertThat(usage.rows().indexOf(rowNamed(usage, UsageTable.ACCOUNT, name)))
				.as("the account's row stands above the guild's")
				.isLessThan(usage.rows().indexOf(rowNamed(usage, UsageTable.GUILD, GUILD)));
		assertThat(shown).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("the admin page lists the accounts under \"Nutzung\" in this order: {quotedNames}")
	public void theAdminPageListsTheAccountsUnderNutzungInThisOrder(List<String> names) {
		List<String> shown = usage().rows().stream().filter(row -> row.kind().equals(UsageTable.ACCOUNT)).map(row -> row.cells().getFirst()).toList();

		assertThat(shown).isEqualTo(names);
	}

	@Then("the admin page lists these accounts, among the others:")
	public void theAdminPageListsTheseAccountsAmongTheOthers(DataTable expected) {
		List<List<String>> shown = adminsBrowser().read("read-admin.js", AdminPage.class).accounts().stream().map(ShownAccount::cells).toList();
		List<List<String>> listed = expected.asLists().stream().skip(1).map(row -> row.stream().map(cell -> cell == null ? "" : cell).toList()).toList();

		assertThat(shown).containsAll(listed);
	}

	@Then("the operator finds {string} as {word}'s {string} in the guild's usage figures, asked for with the admin key")
	public void theOperatorFindsInTheGuildsUsageFigures(String expected, String name, String column) {
		String login = LoginRequests.loginValueIn(logins.logIn("admin", log.adminKeyNamed()));
		service.awaitTheUsageWritten();
		Answer answer = logins.asking(USAGE_FIGURES, login);

		assertThat(answer.status()).as("the usage figures: " + answer.body()).isEqualTo(200);
		assertThat(figureOf(answer, name, column)).isEqualTo(expected);
	}

	private void openTheAdminPage() {
		admins.openTheAdminPage();
	}

	private MemberBrowser adminsBrowser() {
		return admins.current();
	}

	private void awaitTheUsage() {
		adminsBrowser().awaitUntil("document.querySelector('[data-testid=usage]') !== null");
	}

	private UsageTable usage() {
		awaitTheUsage();
		return adminsBrowser().read("read-usage.js", UsageTable.class);
	}

	private UsageTable usageOver(String window) {
		UsageTable usage = usage();
		assertThat(usage.window()).as("the time the usage covers").isEqualTo(window);
		return usage;
	}

	private static UsageTable.UsageTableRow rowNamed(UsageTable usage, String kind, String name) {
		return usage.rows().stream().filter(row -> row.kind().equals(kind) && row.cells().getFirst().equals(name)).findFirst().orElseThrow();
	}

	private static void assertTheAdminKeysFigure(UsageTable usage, String expected, String column) {
		List<UsageTable.UsageTableRow> rows = usage.rows();
		int guild = rows.indexOf(rowNamed(usage, UsageTable.GUILD, GUILD));

		assertThat(rows.get(guild + 1).kind()).as("the row below the guild's").isEqualTo(UsageTable.ADMIN_KEY);
		assertThat(usage.cellOf(rows.get(guild + 1).cells(), column)).isEqualTo(expected);
	}

	private static String figureOf(Answer answer, String name, String column) {
		JsonNode accounts;
		try {
			accounts = JSON.readTree(answer.body()).path("lastThirtyDays").path("accounts");
		} catch (JsonProcessingException e) {
			throw new AssertionError("the usage figures are no JSON: " + answer.body(), e);
		}
		for (JsonNode account : accounts) {
			if (account.path("name").asText().equals(name)) {
				JsonNode counts = account.path("counts");
				if (column.equals("Zuletzt")) {
					return counts.path("latestUse").isNull() ? NEVER : BERLIN_MINUTE.format(Instant.parse(counts.path("latestUse").asText()));
				}
				return counts.path(FIGURES.get(column)).asText();
			}
		}
		throw new AssertionError("the usage figures hold no account " + name + ": " + answer.body());
	}
}
