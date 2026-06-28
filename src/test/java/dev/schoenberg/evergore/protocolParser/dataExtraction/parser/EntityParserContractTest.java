package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.domain.Entry;
import dev.schoenberg.evergore.protocolParser.domain.Item;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;

class EntityParserContractTest {

	@Test
	void parsesHeadlineIntoEntry() {
		Entry entry = parse("01.01.2000 00:00 Hans Meyer Einlagerung");

		assertThat(berlinTime(entry)).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
		assertEntry(entry, "Hans Meyer", EINLAGERUNG);
	}

	@Test
	void parsesWithdrawalEntry() {
		Entry entry = parse("01.01.2000 00:00 Name Entnahme", "1 Item");

		assertEntry(entry, "Name", ENTNAHME, new Item(1, "Item", 100));
	}

	@Test
	void defaultsItemQualityTo100WhenAbsent() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "1 Item");

		assertThat(entry.items()).containsExactly(new Item(1, "Item", 100));
	}

	@Test
	void ignoresThePlusOneQualityModifier() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "1 Item +1");

		assertThat(entry.items()).containsExactly(new Item(1, "Item", 100));
	}

	@Test
	void mergesQuantitiesForSameNameAndQuality() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "2 Item", "3 Item");

		assertThat(entry.items()).containsExactly(new Item(5, "Item", 100));
	}

	@Test
	void keepsItemsSeparateWhenQualityDiffers() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "1 Item (50)", "2 Item (60)");

		assertThat(entry.items()).containsExactlyInAnyOrder(new Item(1, "Item", 50), new Item(2, "Item", 60));
	}

	@Test
	void stopsParsingItemsAtTheImpressumLine() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "1 Item", "Impressum", "2 Other");

		assertThat(entry.items()).containsExactly(new Item(1, "Item", 100));
	}

	@Test
	void splitsProtocolIntoOneEntryPerHeadline() {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "02.02.2002 12:00 Bert Entnahme", "2 Other"));

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@Test
	void returnsNoEntriesForEmptyProtocol() {
		assertThat(EntityParser.parse(List.of())).isEmpty();
	}

	private static Entry parse(String... lines) {
		return EntryFactory.parseContent(List.of(lines));
	}

	private static LocalDateTime berlinTime(Entry entry) {
		return LocalDateTime.ofInstant(entry.date(), APP_ZONE);
	}

	private static void assertEntry(Entry actual, String expectedAvatar, TransferType expectedType, Item... expectedItems) {
		assertThat(actual.avatar()).isEqualTo(expectedAvatar);
		assertThat(actual.type()).isEqualTo(expectedType);
		assertThat(actual.items()).containsExactlyInAnyOrder(expectedItems);
	}
}
