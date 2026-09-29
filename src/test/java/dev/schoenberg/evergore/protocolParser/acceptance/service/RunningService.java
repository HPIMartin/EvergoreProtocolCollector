package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.env.PropertySource;
import io.micronaut.runtime.server.EmbeddedServer;

import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;

public class RunningService {
	private static final String TEST_ENVIRONMENT = "test";
	private static final String OPERATOR_ENVIRONMENT = "the operator's environment variables";
	private static final Map<String, Object> ACCEPTANCE_PROPERTIES = Map.of("micronaut.server.port", -1, "micronaut.health.monitor.enabled", false);

	private final GameProtocols protocols;
	private final ScenarioTime time;
	private final LedgerFaults faults;
	private final ScenarioDatabase database;
	private final ServiceLog log;
	private final OperatorSettings settings;
	private ApplicationContext context;
	private EmbeddedServer server;
	private boolean asDeployed;
	private int collectionsRun;

	public RunningService(GameProtocols protocols, ScenarioTime time, LedgerFaults faults, ScenarioDatabase database, ServiceLog log, OperatorSettings settings) {
		this.protocols = protocols;
		this.time = time;
		this.faults = faults;
		this.database = database;
		this.log = log;
		this.settings = settings;
	}

	public void start() {
		OwnThread.run(() -> startHere(builder().environments(TEST_ENVIRONMENT, AcceptanceEnvironment.NAME)));
	}

	public void restart() {
		stop();
		if (asDeployed) {
			OwnThread.run(() -> startHere(deployed()));
		} else {
			start();
		}
	}

	public Optional<RuntimeException> restartAsDeployed() {
		stop();
		asDeployed = true;
		try {
			OwnThread.run(() -> startHere(deployed()));
			return Optional.empty();
		} catch (RuntimeException refused) {
			stop();
			return Optional.of(refused);
		}
	}

	public void runAsDeployed() {
		if (!asDeployed) {
			restartAsDeployed().ifPresent(refused -> {
				throw refused;
			});
		}
	}

	private ApplicationContextBuilder deployed() {
		Map<String, Object> properties = new HashMap<>(ACCEPTANCE_PROPERTIES);
		properties.putAll(settings.properties());
		return builder()
				.environments(AcceptanceEnvironment.NAME)
				.environmentPropertySource(false)
				.properties(properties)
				.propertySources(PropertySource
						.of(OPERATOR_ENVIRONMENT, settings.environment(), PropertySource.PropertyConvention.ENVIRONMENT_VARIABLE, PropertySource.Origin.of(OPERATOR_ENVIRONMENT)));
	}

	private ApplicationContextBuilder builder() {
		return ApplicationContext.builder().deduceEnvironment(false).properties(ACCEPTANCE_PROPERTIES).singletons(protocols, time, faults, database, log, settings);
	}

	private void startHere(ApplicationContextBuilder builder) {
		server = null;
		context = builder.build();
		context.start();
		server = context.getBean(EmbeddedServer.class).start();
	}

	public void stop() {
		if (context != null && context.isRunning()) {
			OwnThread.run(context::close);
		}
	}

	public boolean isRunning() {
		return context != null && context.isRunning() && server != null && server.isRunning();
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
