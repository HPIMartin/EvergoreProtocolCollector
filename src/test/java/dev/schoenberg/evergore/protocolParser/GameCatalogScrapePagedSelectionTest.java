package dev.schoenberg.evergore.protocolParser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameCatalogScrapePagedSelectionTest {

	@Test
	void aStockOutSelectionWithNoPositionIsPaged() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_out&selection=7");

		assertThat(paged).isTrue();
	}

	@Test
	void aStockOutSelectionWithAnExplicitPositionIsFetchedOnce() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_out&selection=7&pos=2");

		assertThat(paged).isFalse();
	}

	@Test
	void aParameterMerelyEndingInPosIsNotMistakenForAnExplicitPosition() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_out&selection=7&sortpos=name");

		assertThat(paged).isTrue();
	}

	@Test
	void aNonStorageSelectionIsFetchedOnceRegardless() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("academy_craft&selection=52");

		assertThat(paged).isFalse();
	}

	@Test
	void aPageWhoseNameMerelyStartsWithStockOutIsNotMistakenForTheStorageSelection() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_outbound&selection=7");

		assertThat(paged).isFalse();
	}

	@Test
	void anExplicitPositionWithLeadingWhitespaceIsStillRecognizedAsExplicit() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_out&selection=7& pos=2");

		assertThat(paged).isFalse();
	}

	@Test
	void theStorageOverviewWithNoSelectionIsFetchedOnceRatherThanPaged() {
		boolean paged = GameCatalogScrapeCheck.isAPagedStorageSelection("stock_out");

		assertThat(paged).isFalse();
	}
}
