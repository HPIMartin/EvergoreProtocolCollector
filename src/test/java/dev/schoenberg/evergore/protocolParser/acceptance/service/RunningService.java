package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.util.Map;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;

import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;

public class RunningService {
	private static final String TEST_ENVIRONMENT = "test";

	private final GameProtocols protocols;
	private final ScenarioTime time;
	private final LedgerFaults faults;
	private final ScenarioDatabase database;
	private final ServiceLog log;
	private ApplicationContext context;
	private EmbeddedServer server;
	private int collectionsRun;

	public RunningService(GameProtocols protocols, ScenarioTime time, LedgerFaults faults, ScenarioDatabase database, ServiceLog log) {
		this.protocols = protocols;
		this.time = time;
		this.faults = faults;
		this.database = database;
		this.log = log;
	}

	public void start() {
		OwnThread.run(this::startHere);
	}

	private void startHere() {
		context = ApplicationContext
				.builder()
				.deduceEnvironment(false)
				.environments(TEST_ENVIRONMENT, AcceptanceEnvironment.NAME)
				.properties(Map.of("micronaut.server.port", -1, "micronaut.health.monitor.enabled", false))
				.singletons(protocols, time, faults, database, log)
				.start();
		server = context.getBean(EmbeddedServer.class).start();
	}

	public void stop() {
		if (context != null && context.isRunning()) {
			OwnThread.run(context::close);
		}
	}

	public int runTheDailyCollection() {
		ManualTaskScheduler scheduler = context.getBean(ManualTaskScheduler.class);
		collectionsRun++;
		return OwnThread.call(() -> faults.duringCollection(scheduler::runRepeatingJobs));
	}

	public void storeWhatTheGameProtocolsShow() {
		OwnThread.run(context.getBean(EvergoreDataExtractor.class)::loadData);
	}

	public int collectionsRun() {
		return collectionsRun;
	}

	public int port() {
		return server.getPort();
	}
}
