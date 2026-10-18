package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class StartupSteps {
	private static final String THROTTLE = "evergore.rate-limit.";
	private static final int SERVED = 200;
	private static final String RUNS_ALONE = "@time-zone";

	private final OperatorSettings settings;
	private final RunningService service;
	private final ServiceRequests requests;
	private Optional<RuntimeException> refusal = Optional.empty();
	private Optional<TimeZone> zoneBeforeTheScenario = Optional.empty();
	private boolean runsAlone;

	public StartupSteps(OperatorSettings settings, RunningService service, ServiceRequests requests) {
		this.settings = settings;
		this.service = service;
		this.requests = requests;
	}

	@Given("{word} is unset")
	public void isUnset(String variable) {
		settings.unset(variable);
	}

	@Given("{word} is blank")
	public void isBlank(String variable) {
		settings.blank(variable);
	}

	@Given("the service's time zone is {string}")
	public void theServicesTimeZoneIs(String zone) {
		if (!runsAlone) {
			throw new IllegalStateException("A scenario that sets the service's time zone changes the zone of the whole JVM and must be tagged " + RUNS_ALONE);
		}
		zoneBeforeTheScenario = Optional.of(zoneBeforeTheScenario.orElse(TimeZone.getDefault()));
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
	}

	@Before(RUNS_ALONE)
	public void noteThatTheScenarioRunsAlone() {
		runsAlone = true;
	}

	@After(RUNS_ALONE)
	public void restoreTheZoneBeforeTheScenario() {
		zoneBeforeTheScenario.ifPresent(TimeZone::setDefault);
	}

	@Given("the throttle setting {string} is {word}")
	public void theThrottleSettingIs(String setting, String value) {
		settings.set(THROTTLE + setting, value);
	}

	@Given("the service has been restarted since")
	public void theServiceHasBeenRestartedSince() {
		service.restart();
	}

	@When("the operator starts the service")
	public void theOperatorStartsTheService() {
		refusal = service.restartAsDeployed();
	}

	@Then("the service does not start")
	public void theServiceDoesNotStart() {
		assertThat(refusal).isPresent();
		assertThat(service.isRunning()).isFalse();
	}

	@Then("the service starts")
	public void theServiceStarts() {
		assertThat(refusal).isEmpty();
		assertThat(service.isRunning()).isTrue();

		Answer health = requests.get("/health", Map.of());

		assertThat(health.status()).isEqualTo(SERVED);
	}
}
