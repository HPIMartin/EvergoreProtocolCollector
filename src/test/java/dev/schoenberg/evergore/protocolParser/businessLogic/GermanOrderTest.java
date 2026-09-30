package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GermanOrderTest {
	@Test
	void sortsAnUmlautNameWhereGermanCollationPutsItRatherThanBehindZ() {
		List<String> sorted = GermanOrder.distinctSorted(List.of("Zorn", "Ärger", "Anna"));

		assertThat(sorted).containsExactly("Anna", "Ärger", "Zorn");
	}

	@Test
	void namesAnUmlautVariantOfAStemBeforeTheNextLetter() {
		List<String> sorted = GermanOrder.distinctSorted(List.of("Zunder", "Äxtchen", "Übungsstück-Wollrüstung", "Übungsstück-Ätherrüstung"));

		assertThat(sorted).containsExactly("Äxtchen", "Übungsstück-Ätherrüstung", "Übungsstück-Wollrüstung", "Zunder");
	}

	@Test
	void namesANameOccurringTwiceOnce() {
		List<String> sorted = GermanOrder.distinctSorted(List.of("Federn", "Federn", "Achat-Armbrust"));

		assertThat(sorted).containsExactly("Achat-Armbrust", "Federn");
	}

	@Test
	void namesNobodyForNoNames() {
		List<String> sorted = GermanOrder.distinctSorted(List.of());

		assertThat(sorted).isEmpty();
	}
}
