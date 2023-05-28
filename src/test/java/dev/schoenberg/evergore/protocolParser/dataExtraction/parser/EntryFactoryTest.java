package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import static org.assertj.core.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

import dev.schoenberg.evergore.protocolParser.domain.*;

class EntryFactoryTest {

	private List<String> input;

	@BeforeEach
	public void setup() {
		input = new ArrayList<>();
	}

	@Test
	void deduplicates() {
		input.add("11.12.2001 13:37 TestName Einlagerung");
		input.add("5 Drachenhaut");
		input.add("7 Drachenhaut");

		Entry e = EntryFactory.parseContent(input);

		assertThat(e.items).hasSize(1);
	}
}
