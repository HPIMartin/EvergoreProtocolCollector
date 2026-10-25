package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.env.PropertySource;
import io.micronaut.http.server.exceptions.ServerStartupException;
import io.micronaut.runtime.server.EmbeddedServer;

import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioPasswords;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class RunningService {
	private static final String TEST_ENVIRONMENT = "test";
	private static final String OPERATOR_ENVIRONMENT = "the operator's environment variables";
	private static final String SERVER_PORT = "micronaut.server.port";
	private static final Duration PORT_WAIT = Duration.ofSeconds(15);
	private static final Duration PORT_RETRY = Duration.ofMillis(200);
	private static final Map<String, Object> ACCEPTANCE_PROPERTIES = Map.of(SERVER_PORT, -1, "micronaut.health.monitor.enabled", false);

	private final GameProtocols protocols;
	private final ScenarioTime time;
	private final LedgerFaults faults;
	private final ScenarioDatabase database;
	private final ServiceLog log;
	private final OperatorSettings settings;
	private final ScenarioPasswords passwords;
	private ApplicationContext context;
	private EmbeddedServer server;
	private boolean asDeployed;
	private int collectionsRun;

	public RunningService(GameProtocols protocols, ScenarioTime time, LedgerFaults faults, ScenarioDatabase database, ServiceLog log, OperatorSettings settings,
			ScenarioPasswords passwords) {
		this.protocols = protocols;
		this.time = time;
		this.faults = faults;
		this.database = database;
		this.log = log;
		this.settings = settings;
		this.passwords = passwords;
	}

	public void start() {
		OwnThread.run(() -> startHere(builder().environments(TEST_ENVIRONMENT, AcceptanceEnvironment.NAME)));
	}

	public void restart() {
		Map<String, Object> onTheSamePort = new HashMap<>(ACCEPTANCE_PROPERTIES);
		onTheSamePort.put(SERVER_PORT, port());
		stop();
		if (asDeployed) {
			startOnceThePortIsFree(() -> deployed(onTheSamePort));
		} else {
			startOnceThePortIsFree(() -> builder(onTheSamePort).environments(TEST_ENVIRONMENT, AcceptanceEnvironment.NAME));
		}
	}

	private void startOnceThePortIsFree(Supplier<ApplicationContextBuilder> builder) {
		Instant giveUp = Instant.now().plus(PORT_WAIT);
		while (true) {
			try {
				OwnThread.run(() -> startHere(builder.get()));
				return;
			} catch (ServerStartupException portStillHeld) {
				stop();
				if (Instant.now().isAfter(giveUp)) {
					throw portStillHeld;
				}
				silentThrow(() -> Thread.sleep(PORT_RETRY.toMillis()));
			}
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
		return deployed(ACCEPTANCE_PROPERTIES);
	}

	private ApplicationContextBuilder deployed(Map<String, Object> base) {
		Map<String, Object> properties = new HashMap<>(base);
		properties.putAll(settings.properties());
		return builder(base)
				.environments(AcceptanceEnvironment.NAME)
				.environmentPropertySource(false)
				.properties(properties)
				.propertySources(PropertySource
						.of(OPERATOR_ENVIRONMENT, settings.environment(), PropertySource.PropertyConvention.ENVIRONMENT_VARIABLE, PropertySource.Origin.of(OPERATOR_ENVIRONMENT)));
	}

	private ApplicationContextBuilder builder() {
		return builder(ACCEPTANCE_PROPERTIES);
	}

	private ApplicationContextBuilder builder(Map<String, Object> properties) {
		return ApplicationContext.builder().deduceEnvironment(false).properties(properties).singletons(protocols, time, faults, database, log, settings, passwords);
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
