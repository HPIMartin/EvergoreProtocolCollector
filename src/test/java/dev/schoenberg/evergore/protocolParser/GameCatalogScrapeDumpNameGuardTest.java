package dev.schoenberg.evergore.protocolParser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameCatalogScrapeDumpNameGuardTest {

	@Test
	void aDumpNameIsRefusedBeforeItCanOverwriteAnEarlierDump() {
		GameCatalogScrapeCheck check = new GameCatalogScrapeCheck();
		check.recordDumpName("page-stock-out-selection-7-pos-1");

		assertThatThrownBy(() -> check.recordDumpName("page-stock-out-selection-7-pos-1")).isInstanceOf(AssertionError.class);
	}
}
