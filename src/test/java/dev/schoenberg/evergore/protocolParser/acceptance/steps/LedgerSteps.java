package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.IntStream;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.MINUTE;

public class LedgerSteps {
	private static final LocalDateTime FIRST_MOVEMENT = LocalDateTime.of(2026, 1, 1, 12, 0);

	private final GameProtocols protocols;
	private final Guild guild;
	private final RunningService service;

	public LedgerSteps(GameProtocols protocols, Guild guild, RunningService service) {
		this.protocols = protocols;
		this.guild = guild;
		this.service = service;
	}

	@Given("the guild bank ledger (also )holds:")
	public void theGuildBankLedgerHolds(DataTable movements) {
		for (Map<String, String> movement : movements.asMaps()) {
			protocols.bank().record(minuteOf(movement), avatarOf(movement), movement.get("Vorgang"), movement.get("Betrag") + " Gold");
		}
		service.storeWhatTheGameProtocolsShow();
	}

	@Given("the guild storage ledger (also )holds:")
	public void theGuildStorageLedgerHolds(DataTable movements) {
		for (Map<String, String> movement : movements.asMaps()) {
			String line = movement.get("Menge") + " " + movement.get("Gegenstand") + " (" + movement.get("Qualität") + ")";
			protocols.storage().record(minuteOf(movement), avatarOf(movement), movement.get("Vorgang"), line);
		}
		service.storeWhatTheGameProtocolsShow();
	}

	@Given("the guild ledgers hold no movement yet")
	public void theGuildLedgersHoldNoMovementYet() {
		protocols.newestMinute().ifPresent(minute -> {
			throw new IllegalStateException("The scenario put a movement of " + minute + " into the ledgers before stating that they hold none");
		});
	}

	@Given("{int} members have each paid {int} gold into the guild bank")
	public void membersHaveEachPaidGoldIntoTheGuildBank(int members, int gold) {
		IntStream.rangeClosed(1, members).mapToObj("Mitglied %03d"::formatted).forEach(member -> {
			guild.join(member);
			protocols.bank().record(FIRST_MOVEMENT, member, "Einzahlung", gold + " Gold");
		});
		service.storeWhatTheGameProtocolsShow();
	}

	private String avatarOf(Map<String, String> movement) {
		String avatar = movement.get("Avatar");
		guild.join(avatar);
		return avatar;
	}

	private static LocalDateTime minuteOf(Map<String, String> movement) {
		return LocalDateTime.parse(movement.get("Zeitpunkt"), MINUTE);
	}
}
