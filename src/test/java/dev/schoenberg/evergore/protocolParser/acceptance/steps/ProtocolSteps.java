package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;

public class ProtocolSteps {
	private final GameProtocols protocols;
	private final LedgerFaults faults;
	private final CollectionSteps collection;

	public ProtocolSteps(GameProtocols protocols, LedgerFaults faults, CollectionSteps collection) {
		this.protocols = protocols;
		this.faults = faults;
		this.collection = collection;
	}

	@Given("the game's {ledger} protocol shows:")
	public void theGamesProtocolShows(LedgerName protocol, String text) {
		List<String> lines = text.lines().toList();
		if (protocol == BANK) {
			protocols.bank().show(lines);
		} else {
			protocols.storage().show(lines);
		}
	}

	@Given("the game's protocols show no entry")
	public void theGamesProtocolsShowNoEntry() {
		protocols.showNoEntry();
	}

	@Given("the game cannot be reached")
	public void theGameCannotBeReached() {
		protocols.cutOff();
	}

	@Given("the game can be reached again")
	public void theGameCanBeReachedAgain() {
		protocols.reconnect();
	}

	@Given("the guild's figures cannot be saved")
	public void theGuildsFiguresCannotBeSaved() {
		faults.refuseToSaveFigures();
	}

	@Given("the guild's figures can be saved again")
	public void theGuildsFiguresCanBeSavedAgain() {
		faults.saveFiguresAgain();
	}

	@When("the daily collection runs")
	public void theDailyCollectionRuns() {
		collection.theDailyCollectionHasRun();
	}
}
