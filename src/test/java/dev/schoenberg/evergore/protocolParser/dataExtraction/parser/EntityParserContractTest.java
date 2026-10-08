package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.domain.Entry;
import dev.schoenberg.evergore.protocolParser.domain.Item;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

	@ParameterizedTest
	@MethodSource("headlinesWithSingleDigitDateFields")
	void doesNotFoldTheItemsOfAHeadlineWithSingleDigitDateFieldsIntoThePrecedingEntry(String headline) {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", headline, "5 Ghost"), logger);

		assertThat(entries).hasSize(1);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
	}

	@ParameterizedTest
	@MethodSource("headlinesWithSingleDigitDateFields")
	void warnsAboutADroppedHeadlineWithSingleDigitDateFields(String headline) {
		EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", headline, "5 Ghost"), logger);

		assertThat(logger.warnMessages()).containsExactly("Dropping protocol entry: malformed headline: " + headline);
	}

	@ParameterizedTest
	@MethodSource("itemLinesThatReadLikeADateAndTime")
	void keepsAnItemLineThatReadsLikeADateAndTimeInItsEntry(String itemLine, Item item) {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", itemLine, "1 Later"), logger);

		assertThat(entries).hasSize(1);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, item, new Item(1, "Later", 100));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.1.2000x1:30", "1.1.2000 1x30", "100.1.2000 1:30", "1.100.2000 1:30", ".1.2000 1:30", "1..2000 1:30", "1.1.20000 1:30", "1.1.200 1:30",
			"1.1.20001:30", "1.1.2000\t1:30", "1.1.2000 100:30", "1.1.2000 :30", "1.1.2000 1:", "1.1.2000 1:x", "123.2026 1:30"})
	void keepsTheLinesAfterALineShapedAlmostLikeAHeadlineInTheirEntry(String line) {
		List<Entry> entries = EntityParser.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", line, "2 Later"), logger);

		assertThat(entries).hasSize(1);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100), new Item(2, "Later", 100));
		assertThat(logger.warnMessages()).isEmpty();
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
	void doesNotMintAnEntryFromATypeWordInsideAnAvatarName() {
		List<Entry> entries = EntityParser
				.parse(List.of("01.01.2000 00:00 Anna Einlagerung", "1 Item", "02.02.2002 12:00 Entnahmefreund Auszahlung", "5 Ghost", "03.03.2003 12:00 Bert Entnahme", "2 Other"),
						logger);

		assertThat(entries).hasSize(2);
		assertEntry(entries.get(0), "Anna", EINLAGERUNG, new Item(1, "Item", 100));
		assertEntry(entries.get(1), "Bert", ENTNAHME, new Item(2, "Other", 100));
	}

	@ParameterizedTest
	@ValueSource(strings = {"01.01.2000 00:00 Entnahmefreund Auszahlung", "01.01.2000 00:00 Anna Entnahmeübersicht", "01.01.2000 00:00 XX-Entnahme-XX",
			"01.01.2000 00:00 BobEntnahme"})
	void dropsAHeadlineWhoseTypeWordIsNoWhitespaceDelimitedToken(String headline) {
		Optional<Entry> entry = EntryFactory.parseContent(List.of(headline, "5 Ghost"), logger);

		assertThat(entry).isEmpty();
		assertThat(logger.warnMessages()).containsExactly("Dropping protocol entry: unmatched transfer type in headline: " + headline);
	}

	@Test
	void keepsAnAvatarNameThatContainsATypeWord() {
		Entry entry = parse("01.01.2000 00:00 Entnahmefreund Einlagerung", "1 Item");

		assertEntry(entry, "Entnahmefreund", EINLAGERUNG, new Item(1, "Item", 100));
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

	private static Stream<String> headlinesWithSingleDigitDateFields() {
		return Stream
				.of("2.12.2001 13:37 Carl Einlagerung", "11.2.2001 13:37 Carl Einlagerung", "11.12.2001 3:37 Carl Einlagerung", "11.12.2001 13:7 Carl Einlagerung",
						"1.1.2000 0:00 Carl Einlagerung", "2/12/2001 13:37 Carl Einlagerung", "2.12X2001 13:37 Carl Einlagerung", "2X12.2001 13:37 Carl Einlagerung",
						"2-12-2001 13:37 Carl Einlagerung", "2\t12.2001 13:37 Carl Einlagerung", "1.1.2000 1:3");
	}

	private static Stream<Arguments> itemLinesThatReadLikeADateAndTime() {
		return Stream
				.of(arguments("2000 2026 10:10", new Item(2000, "2026 10:10", 100)), arguments("100 2026 1:12 Pfeil", new Item(100, "2026 1:12 Pfeil", 100)),
						arguments("5 5 2000 1:30 Pfeil", new Item(5, "5 2000 1:30 Pfeil", 100)), arguments("5 5.2026 1:30 Pfeil", new Item(5, "5.2026 1:30 Pfeil", 100)));
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
