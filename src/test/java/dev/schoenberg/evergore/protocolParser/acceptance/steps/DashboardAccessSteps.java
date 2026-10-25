package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.stream.IntStream;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.BASE_MOVEMENT;

public class DashboardAccessSteps {
	private final GameProtocols protocols;
	private final Guild guild;
	private final RunningService service;

	public DashboardAccessSteps(GameProtocols protocols, Guild guild, RunningService service) {
		this.protocols = protocols;
		this.guild = guild;
		this.service = service;
	}

	@Given("{word} has {int} movements in the guild bank ledger")
	public void hasMovementsInTheGuildBankLedger(String avatar, int count) {
		guild.join(avatar);
		IntStream.rangeClosed(1, count).forEach(minute -> protocols.bank().record(BASE_MOVEMENT.plusMinutes(minute), avatar, "Einzahlung", minute + " Gold"));
		service.storeWhatTheGameProtocolsShow();
	}
}
