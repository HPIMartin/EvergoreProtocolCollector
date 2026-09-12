package dev.schoenberg.evergore.protocolParser;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameCatalogScrapePageRequestsTest {

	@Test
	void everyPositionUpToTheLastIsRequestedExactlyOnceAndNoneBeyondIt() {
		List<String> pageRequests = GameCatalogScrapeCheck.pageRequestsFor("stock_out&selection=7", "Auswahl (Gesamt: 97)");

		assertThat(pageRequests)
				.containsExactly("stock_out&selection=7&pos=1", "stock_out&selection=7&pos=2", "stock_out&selection=7&pos=3", "stock_out&selection=7&pos=4",
						"stock_out&selection=7&pos=5");
	}

	@Test
	void aSelectionThatFitsOnOnePageIsRequestedOnce() {
		List<String> pageRequests = GameCatalogScrapeCheck.pageRequestsFor("stock_out&selection=7", "Auswahl (Gesamt: 20)");

		assertThat(pageRequests).containsExactly("stock_out&selection=7&pos=1");
	}
}
