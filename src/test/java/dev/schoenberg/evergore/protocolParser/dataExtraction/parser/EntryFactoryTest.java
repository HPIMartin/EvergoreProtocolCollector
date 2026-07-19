package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.domain.Entry;

import static org.assertj.core.api.Assertions.assertThat;

class EntryFactoryTest {

	private List<String> input;
	private LoggerSpy logger;

	@BeforeEach
	public void setup() {
		input = new ArrayList<>();
		logger = new LoggerSpy();
	}

	@Test
	void deduplicates() {
		input.add("11.12.2001 13:37 TestName Einlagerung");
		input.add("5 Drachenhaut");
		input.add("7 Drachenhaut");

		Entry e = EntryFactory.parseContent(input, logger).orElseThrow();

		assertThat(e.items()).hasSize(1);
	}

	@Test
	void warnsAndDropsATimestampedHeadlineWithNoKnownTransferType() {
		String headline = "01.01.2000 00:00 Name Auszahlung";
		input.add(headline);
		input.add("1 Gold");

		Optional<Entry> entry = EntryFactory.parseContent(input, logger);

		assertThat(entry).isEmpty();
		assertThat(logger.warnMessages()).containsExactly("Dropping protocol entry: unmatched transfer type in headline: " + headline);
	}
}
