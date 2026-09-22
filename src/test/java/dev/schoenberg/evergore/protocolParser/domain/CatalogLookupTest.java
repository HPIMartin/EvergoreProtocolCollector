package dev.schoenberg.evergore.protocolParser.domain;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.HOLZFAELLERAXT;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.OBSIDIAN_PIKE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STREITAXT;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogLookupTest {
	@Test
	void findsAnItemUnderTheNameTheCatalogGivesIt() {
		assertThat(CatalogLookup.itemFor("Streitaxt")).contains(STREITAXT);
	}

	@Test
	void findsAnItemUnderASecondSpellingTheLedgerUsesForIt() {
		assertThat(CatalogLookup.itemFor("Obsidian-Pike [2H]")).contains(OBSIDIAN_PIKE);
	}

	@Test
	void findsAnItemWhoseNameCarriesAMagicAffix() {
		assertThat(CatalogLookup.itemFor("Streitaxt des Wegelagerers")).contains(STREITAXT);
	}

	@Test
	void findsATwoHandedItemWhoseMagicAffixSitsBeforeItsSuffix() {
		assertThat(CatalogLookup.itemFor("Holzfälleraxt der Entschlossenheit")).contains(HOLZFAELLERAXT);
	}

	@Test
	void findsNothingForANameTheCatalogHasNeverSeen() {
		assertThat(CatalogLookup.itemFor("Vorschlaghammer des Nichts")).isEmpty();
	}
}
