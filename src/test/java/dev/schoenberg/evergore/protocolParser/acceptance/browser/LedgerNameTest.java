package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerNameTest {
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {"Aurora|/avatars/Aurora/bank", "O'Neil|/avatars/O'Neil/bank", "Lady Aurora|/avatars/Lady%20Aurora/bank",
			"Müller|/avatars/M%C3%BCller/bank", "Dr.Who|/avatars/Dr.Who/bank", "A(b)!~|/avatars/A(b)!~/bank", "Q&A?#|/avatars/Q%26A%3F%23/bank", "a+b|/avatars/a%2Bb/bank"})
	void encodesAMembersNameLikeTheSinglePageAppDoes(String member, String path) {
		String encoded = LedgerName.BANK.pathOf(member);

		assertThat(encoded).isEqualTo(path);
	}
}
