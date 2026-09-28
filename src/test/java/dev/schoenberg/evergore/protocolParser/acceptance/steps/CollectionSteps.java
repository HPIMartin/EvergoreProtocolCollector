package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectionSteps {
	private static final LocalTime COLLECTION_TIME = LocalTime.of(5, 0);
	private static final LocalDate FIRST_DAY = LocalDate.of(2026, 1, 1);

	private final RunningService service;
	private final GameProtocols protocols;
	private final ScenarioTime time;
	private final LedgerFaults faults;

	public CollectionSteps(RunningService service, GameProtocols protocols, ScenarioTime time, LedgerFaults faults) {
		this.service = service;
		this.protocols = protocols;
		this.time = time;
		this.faults = faults;
	}

	@Given("the daily collection has run")
	public void theDailyCollectionHasRun() {
		LocalDate newestDay = protocols.newestMinute().map(LocalDateTime::toLocalDate).orElse(FIRST_DAY);
		theDailyCollectionRanAt(newestDay.plusDays(1).atTime(COLLECTION_TIME));
	}

	@Given("the daily collection ran at {moment}")
	public void theDailyCollectionRanAt(LocalDateTime moment) {
		time.setTo(moment);
		int collections = service.runTheDailyCollection();

		assertThat(collections).as("the daily collections the service scheduled").isEqualTo(1);
	}

	@Given("no collection has ever run")
	public void noCollectionHasEverRun() {
		int collections = service.collectionsRun();

		assertThat(collections).isZero();
	}

	@Given("{word}'s stored movements cannot be read")
	public void storedMovementsCannotBeRead(String member) {
		faults.makeUnreadable(member);
	}

	@Given("{word}'s stored movements can be read again")
	public void storedMovementsCanBeReadAgain(String member) {
		faults.makeReadable(member);
	}

	@Given("the figures cannot be loaded")
	public void theFiguresCannotBeLoaded() {
		faults.withholdFigures();
	}
}
