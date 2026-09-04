package dev.schoenberg.evergore.protocolParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSnapshotCheckOptInTest {
	private static final String OPT_IN_PROPERTY = "prodSnapshot.check";

	@Test
	void forwardsTheOptInPropertyFromTheBuildIntoTheTestJvm() {
		assertThat(System.getProperty(OPT_IN_PROPERTY)).as("the build must forward %s, or the on-demand check can never be switched on", OPT_IN_PROPERTY).isNotNull();
	}

	@Test
	void keepsTheProductionSnapshotCheckOffUntilThatSamePropertyIsTrue() {
		EnabledIfSystemProperty condition = ProductionSnapshotRecomputeCheck.class.getAnnotation(EnabledIfSystemProperty.class);

		assertThat(condition).as("the check must carry an opt-in condition instead of being disabled outright").isNotNull();
		assertThat(condition.named()).isEqualTo(OPT_IN_PROPERTY);
		assertThat(condition.matches()).isEqualTo("true");
	}

	@Test
	void leavesTheOptInOffInAnOrdinaryRun() {
		assertThat(System.getProperty(OPT_IN_PROPERTY)).isNotEqualTo("true");
	}
}
