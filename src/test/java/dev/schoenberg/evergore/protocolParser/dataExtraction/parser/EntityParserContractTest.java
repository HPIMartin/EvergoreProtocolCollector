package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.domain.Entry;
import dev.schoenberg.evergore.protocolParser.domain.Item;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;

class EntityParserContractTest {

	private LoggerSpy logger;

	@BeforeEach
	void setup() {
		logger = new LoggerSpy();
	}

	@Test
	void parsesHeadlineIntoEntry() {
		Entry entry = parse("01.01.2000 00:00 Hans Meyer Einlagerung");

		assertThat(berlinTime(entry)).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
		assertEntry(entry, "Hans Meyer", EINLAGERUNG);
	}

	@Test
	void mapsTheEinzahlungHeadlineToEinlagerung() {
		Entry entry = parse("01.01.2000 00:00 Hans Meyer Einzahlung");

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
	void mergesAPlusOneModifiedItemWithItsUnmodifiedCounterpart() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "3 X +1", "5 X");

		assertThat(entry.items()).containsExactly(new Item(8, "X", 100));
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
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "02.02.2002 12:00 Bert Entnahme", "2 Other"), logger);

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@Test
	void returnsNoEntriesForEmptyProtocol() {
		assertThat(EntityParser.parse(List.of(), logger)).isEmpty();
	}

	@Test
	void skipsAHeadlineWithAMalformedDateAndStillParsesNeighbouringEntries() {
		List<Entry> entries = EntityParser
				.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "11.12X2001 13:37 Bad Einlagerung", "02.02.2002 12:00 Bert Entnahme", "2 Other"), logger);

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@Test
	void doesNotFoldAMalformedHeadlinesItemsIntoThePrecedingEntry() {
		List<Entry> entries = EntityParser
				.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "11.12X2001 13:37 Bad Einlagerung", "5 Ghost", "02.02.2002 12:00 Bert Entnahme", "2 Other"), logger);

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@Test
	void doesNotAbortTheIngestOnAnOutOfRangeDate() {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "31.13.2001 25:99 Bad Einlagerung", "5 Ghost"), logger);

		assertThat(entries).hasSize(1);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
	}

	@Test
	void skipsAnItemLineWithoutAnAmountAndKeepsTheOtherItems() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", " Ghost", "2 Item");

		assertThat(entry.items()).containsExactly(new Item(2, "Item", 100));
	}

	@Test
	void skipsAnItemLineWhoseQualityExceedsTheNumberRangeAndKeepsTheOtherItems() {
		Entry entry = parse("01.01.2000 00:00 Name Einlagerung", "1 Ghost (99999999999)", "2 Item");

		assertThat(entry.items()).containsExactly(new Item(2, "Item", 100));
	}

	@Test
	void warnsAboutASkippedItemLineWithoutAnAmount() {
		EntryFactory.parseContent(List.of("01.01.2000 00:00 Name Einlagerung", " Ghost", "2 Item"), logger);

		assertThat(logger.warnMessages()).containsExactly("Skipping item line with an unparseable number:  Ghost");
	}

	@Test
	void doesNotAbortTheIngestOnAnItemLineWithoutAnAmount() {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", " Ghost", "02.02.2002 12:00 Bert Entnahme", "2 Other"), logger);

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG);
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@Test
	void warnsWhenARecognizedEntryYieldsNoParseableItems() {
		EntryFactory.parseContent(List.of("01.01.2000 00:00 Name Einlagerung", "1.000 Gold"), logger);

		assertThat(logger.warnMessages()).containsExactly("Protocol entry yielded no parseable items: 01.01.2000 00:00 Name Einlagerung");
	}

	@Test
	void warnsWhenABlockHeadFailingTheStrictHeadlineMatchIsDropped() {
		String headline = "11.12X2001 13:37 Bad Einlagerung";

		EntryFactory.parseContent(List.of(headline, "5 Ghost"), logger);

		assertThat(logger.warnMessages()).containsExactly("Dropping protocol entry: malformed headline: " + headline);
	}

	@Test
	void warnsWhenATypedHeadlineWithAnOutOfRangeDateIsDropped() {
		EntryFactory.parseContent(List.of("31.13.2001 25:99 Bad Einlagerung", "5 Ghost"), logger);

		assertThat(logger.warnMessages()).containsExactly("Dropping protocol entry: out-of-range date in headline: 31.13.2001 25:99 Bad Einlagerung");
	}

	private Entry parse(String... lines) {
		return EntryFactory.parseContent(List.of(lines), logger).orElseThrow();
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
