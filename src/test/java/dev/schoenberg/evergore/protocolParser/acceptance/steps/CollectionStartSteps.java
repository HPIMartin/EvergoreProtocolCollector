package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;
import java.util.List;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.Asking;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.CollectionRuns;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ViewedBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectionStartSteps {
	private static final String ADMIN_PAGE = "/admin";
	private static final String COLLECTION = "/api/v1/admin/collection";
	private static final String READ_ADMIN = "read-admin.js";
	private static final int REFUSED_AS_CONFLICTING = 409;

	private final RunningService service;
	private final GameProtocols protocols;
	private final LedgerFaults faults;
	private final ScenarioTime time;
	private final ServiceLog log;
	private final CollectionRuns runs;
	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final ViewedBrowser viewed;
	private final LoginRequests logins;

	public CollectionStartSteps(RunningService service, GameProtocols protocols, LedgerFaults faults, ScenarioTime time, ServiceLog log, CollectionRuns runs, MemberBrowser browser,
			SecondBrowser secondBrowser, ViewedBrowser viewed, LoginRequests logins) {
		this.service = service;
		this.protocols = protocols;
		this.faults = faults;
		this.time = time;
		this.log = log;
		this.runs = runs;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
		this.viewed = viewed;
		this.logins = logins;
	}

	@ParameterType(".+")
	public String restOfLine(String text) {
		return text;
	}

	@When("the admin starts a collection on the admin page at {moment}")
	public void theAdminStartsACollectionOnTheAdminPage(LocalDateTime moment) {
		startOnTheAdminPage(browser, moment);
	}

	@Given("the admin started a collection on the admin page at {moment}")
	public void theAdminStartedACollectionOnTheAdminPage(LocalDateTime moment) {
		startOnTheAdminPage(browser, moment);
	}

	@Given("the admin started a collection on the admin page at {moment} and that collection has finished")
	public void theAdminStartedACollectionAndItHasFinished(LocalDateTime moment) {
		startOnTheAdminPage(browser, moment);
		thatCollectionFinishes();
	}

	@Given("the admin started a collection on the admin page at {moment} and it finished {int} minutes later")
	public void theAdminStartedACollectionAndItFinishedLater(LocalDateTime moment, int minutes) {
		startOnTheAdminPage(browser, moment);
		time.setTo(moment.plusMinutes(minutes));
		thatCollectionFinishes();
	}

	@Given("the game cannot be reached and the admin started a collection on the admin page at {moment} and it finished {int} minutes later")
	public void theGameCannotBeReachedAndTheAdminStartedACollection(LocalDateTime moment, int minutes) {
		protocols.cutOff();
		theAdminStartedACollectionAndItFinishedLater(moment, minutes);
	}

	@Given("the guild's figures cannot be saved and the admin started a collection on the admin page at {moment} and it finished {int} minutes later")
	public void theFiguresCannotBeSavedAndTheAdminStartedACollection(LocalDateTime moment, int minutes) {
		faults.refuseToSaveFigures();
		theAdminStartedACollectionAndItFinishedLater(moment, minutes);
	}

	@Given("the admin started a collection on the admin page at {moment} and it finished {int} minutes after its start, and then the daily collection fell due at {moment} and finished")
	public void theAdminStartedACollectionAndThenTheDailyOneFellDue(LocalDateTime moment, int minutes, LocalDateTime daily) {
		theAdminStartedACollectionAndItFinishedLater(moment, minutes);
		theDailyCollectionFallsDueAndFinishes(daily);
	}

	@Given("the admin started a collection on the admin page at {moment}, the daily collection fell due at {moment} while it was running and was skipped, and it finished {int} minutes after its start")
	public void theAdminStartedACollectionAndTheDailyOneWasSkipped(LocalDateTime moment, LocalDateTime daily, int minutes) {
		startOnTheAdminPage(browser, moment);
		theDailyCollectionFallsDueWhileThatCollectionRuns(daily);
		time.setTo(moment.plusMinutes(minutes));
		thatCollectionFinishes();
	}

	@Given("that collection has finished")
	public void thatCollectionHasFinished() {
		thatCollectionFinishes();
	}

	@When("that collection finishes")
	public void thatCollectionFinishes() {
		protocols.release();
		service.awaitTheCollectionsStartedByHand();
		service.awaitTheDailyCollectionInTheBackground();
	}

	@Given("the daily collection fell due at {moment}")
	public void theDailyCollectionFellDue(LocalDateTime moment) {
		startTheDailyCollectionHeld(moment);
	}

	@When("the daily collection falls due at {moment} and the admin opens the admin page while it runs")
	public void theDailyCollectionFallsDueAndTheAdminOpensTheAdminPage(LocalDateTime moment) {
		startTheDailyCollectionHeld(moment);
		openTheAdminPage();
	}

	@When("the daily collection falls due at {moment} while that collection runs")
	public void theDailyCollectionFallsDueWhileThatCollectionRuns(LocalDateTime moment) {
		time.setTo(moment);
		assertThat(service.runTheDailyCollection()).as("the daily collections the service scheduled").isEqualTo(1);
	}

	@Given("the daily collection fell due at {moment} while that collection was running")
	public void theDailyCollectionFellDueWhileThatCollectionWasRunning(LocalDateTime moment) {
		theDailyCollectionFallsDueWhileThatCollectionRuns(moment);
	}

	@When("the daily collection falls due at {moment} and finishes")
	public void theDailyCollectionFallsDueAndFinishes(LocalDateTime moment) {
		time.setTo(moment);
		runs.dailyStarted(moment, log.lines().size());
		assertThat(service.runTheDailyCollection()).as("the daily collections the service scheduled").isEqualTo(1);
	}

	@Given("the daily collection was the last to start")
	public void theDailyCollectionWasTheLastToStart() {
		assertThat(runs.dailyStart()).isNotNull();
	}

	@Given("the daily collection was the last to start, and a start by hand was refused {int} minutes after it for coming within the hour")
	public void theDailyCollectionWasTheLastToStartAndAStartByHandWasRefused(int minutes) {
		time.setTo(runs.dailyStart().plusMinutes(minutes));
		Answer answer = askForACollectionWithTheAdminKey();

		assertThat(answer.status()).as("the answer to the start by hand: " + answer.body()).isEqualTo(REFUSED_AS_CONFLICTING);
	}

	@Given("the service has been restarted {int} minutes after the start of the daily collection")
	public void theServiceHasBeenRestarted(int minutes) {
		time.setTo(runs.dailyStart().plusMinutes(minutes));
		service.restart();
	}

	@Given("the admin has opened the admin page while that collection was running")
	public void theAdminHasOpenedTheAdminPageWhileThatCollectionWasRunning() {
		secondBrowser.browser().opening().openAsTheAdmin(ADMIN_PAGE);
	}

	@When("the admin opens the admin page {int} minutes after starting a collection on it at {moment}, while it runs")
	public void theAdminOpensTheAdminPageAfterStartingACollectionOnIt(int minutes, LocalDateTime moment) {
		startOnTheAdminPage(browser, moment);
		time.setTo(moment.plusMinutes(minutes));
		openTheAdminPage();
	}

	@When("the admin opens the admin page {int} minutes after the start of {restOfLine}")
	public void theAdminOpensTheAdminPageAfterTheStartOf(int minutes, String collection) {
		time.setTo(runs.startNamed(collection).plusMinutes(minutes));
		openTheAdminPage();
	}

	@Then("the admin page offers {string}")
	public void theAdminPageOffers(String button) {
		assertThat(shownOn(browser).startButton()).isEqualTo(button);
	}

	@Then("the admin page offers no {string}")
	public void theAdminPageOffersNo(String button) {
		assertThat(shownOn(browser).startButton()).isNotEqualTo(button);
	}

	@Then("the admin page reads no {string}")
	public void theAdminPageReadsNo(String text) {
		assertThat(shownOn(browser).messages()).noneMatch(message -> message != null && message.contains(text));
	}

	@Then("the page opened before still reads {string}")
	public void thePageOpenedBeforeStillReads(String message) {
		assertThat(shownOn(secondBrowser.browser()).messages()).contains(message);
	}

	@Then("the page opened before still offers no {string}")
	public void thePageOpenedBeforeStillOffersNo(String button) {
		assertThat(shownOn(secondBrowser.browser()).startButton()).isNotEqualTo(button);
	}

	@Then("the operator finds one line {string} in the service's log")
	public void theOperatorFindsOneLineInTheServicesLog(String line) {
		assertThat(log.lines()).filteredOn(line::equals).hasSize(1);
	}

	@Then("the operator finds one line {string} in the service's log since the start of that collection")
	public void theOperatorFindsOneLineSinceTheStartOfThatCollection(String line) {
		assertThat(linesSince(runs.lastStart())).filteredOn(line::equals).hasSize(1);
	}

	@Then("the operator finds no line {string} in the service's log since the start of that collection")
	public void theOperatorFindsNoLineSinceTheStartOfThatCollection(String line) {
		assertThat(linesSince(runs.lastStart())).filteredOn(line::equals).isEmpty();
	}

	@Then("the operator finds one line {string} in the service's log since the start of the collection of {moment}")
	public void theOperatorFindsOneLineSinceTheStartOfTheCollectionOf(String line, LocalDateTime start) {
		assertThat(linesSince(start)).filteredOn(line::equals).hasSize(1);
	}

	private void startOnTheAdminPage(MemberBrowser startingBrowser, LocalDateTime moment) {
		time.setTo(moment);
		protocols.holdTheNextAnswer();
		runs.startedByHand(moment, log.lines().size());
		startingBrowser.opening().openAsTheAdmin(ADMIN_PAGE);
		startingBrowser.collectionStart().click();
		protocols.awaitAsked();
		viewed.lookAt(startingBrowser);
	}

	private void startTheDailyCollectionHeld(LocalDateTime moment) {
		time.setTo(moment);
		protocols.holdTheNextAnswer();
		runs.dailyStarted(moment, log.lines().size());
		service.startTheDailyCollectionInTheBackground();
		protocols.awaitAsked();
	}

	private void openTheAdminPage() {
		browser.opening().openAsTheAdmin(ADMIN_PAGE);
		viewed.lookAt(browser);
	}

	private Answer askForACollectionWithTheAdminKey() {
		Answer loggedIn = logins.logIn("admin", log.adminKeyNamed());

		return logins.asking(Asking.post(COLLECTION), LoginRequests.loginValueIn(loggedIn));
	}

	private List<String> linesSince(LocalDateTime start) {
		return log.lines().subList(runs.logSizeAtStartOf(start), log.lines().size());
	}

	private static AdminPage shownOn(MemberBrowser on) {
		return on.read(READ_ADMIN, AdminPage.class);
	}
}
