package dev.schoenberg.evergore.protocolParser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaBundlePackagingTest {

	@Test
	void jarClasspathContainsTheBundledSpaShell() {
		assertThat(getClass().getResourceAsStream("/static/ui/index.html")).isNotNull();
	}
}
