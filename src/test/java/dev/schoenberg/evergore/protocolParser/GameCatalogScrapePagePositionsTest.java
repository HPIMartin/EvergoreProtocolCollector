package dev.schoenberg.evergore.protocolParser;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameCatalogScrapePagePositionsTest {

	@Test
	void aPageWithNoGesamtCountFailsLoudlyInsteadOfReadingOnePageSilently() {
		assertThatThrownBy(() -> GameCatalogScrapeCheck.pagePositions("Auswahl ohne Gesamtangabe")).isInstanceOf(AssertionError.class).hasMessageContaining("(Gesamt: N)");
	}

	@Test
	void aGesamtCountThatExactlyFillsOnePageStaysOneEntryLong() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 20)");

		assertThat(positions).containsExactly(1);
	}

	@Test
	void aGesamtCountOneRowOverAPageAddsASecondEntry() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 21)");

		assertThat(positions).containsExactly(1, 2);
	}

	@Test
	void aGesamtCountOfNinetySevenStopsAtTheFifthEntry() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 97)");

		assertThat(positions).containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	void aGesamtCountOfZeroStillReadsTheOnePageItCameFrom() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Leere Auswahl (Gesamt: 0)");

		assertThat(positions).containsExactly(1);
	}

	@Test
	void aGermanThousandsSeparatedGesamtCountIsReadInFull() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 1.234)");

		assertThat(positions).hasSize(62);
	}

	@Test
	void aGesamtMentionOutsideParenthesesIsNotMistakenForTheSelectionTotal() {
		List<Integer> positions = GameCatalogScrapeCheck.pagePositions("Gildenkasse Gesamt: 5 Gold\n\nSchmuck (Gesamt: 97)");

		assertThat(positions).containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	void aTotalTooLongForTheCaptureFailsAsIfThePageDidNotRender() {
		assertThatThrownBy(() -> GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 3.000.000.000)")).isInstanceOf(AssertionError.class).hasMessageContaining("(Gesamt: N)");
	}

	@Test
	void aRowCountFarBeyondPlausibleIsRejectedBeforeItCanOverflowThePageCount() {
		assertThatThrownBy(() -> GameCatalogScrapeCheck.pagePositions("Auswahl (Gesamt: 9.999.999)")).isInstanceOf(AssertionError.class).hasMessageContaining("implausible");
	}
}
