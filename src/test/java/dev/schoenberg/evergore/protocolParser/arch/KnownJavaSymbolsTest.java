package dev.schoenberg.evergore.protocolParser.arch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnownJavaSymbolsTest {

	@Test
	void resolvesAClassFromThisProjectsOwnSource() {
		KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

		boolean resolved = tested.hasSimpleName("EvergoreDataEvaluator");

		assertThat(resolved).isTrue();
	}

	@Test
	void resolvesADependencyClassNeverUsedInThisProjectsOwnSource() {
		KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

		boolean resolved = tested.hasSimpleName("RemoteWebDriver");

		assertThat(resolved).isTrue();
	}

	@Test
	void doesNotResolveAMadeUpClassName() {
		KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

		boolean resolved = tested.hasSimpleName("DatabaseStartupInitialization");

		assertThat(resolved).isFalse();
	}
}
