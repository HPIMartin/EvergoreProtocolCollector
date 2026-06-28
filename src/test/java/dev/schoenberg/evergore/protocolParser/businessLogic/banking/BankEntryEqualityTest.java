package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;

class BankEntryEqualityTest {

	private static final Instant TIME = Instant.parse("2001-12-11T13:37:00Z");
	private static final Instant OTHER_TIME = Instant.parse("2002-01-01T00:00:00Z");

	@Test
	void equalWhenAllFieldsMatch() {
		assertThat(reference()).isEqualTo(reference()).hasSameHashCodeAs(reference());
	}

	@Test
	void differsByTimestamp() {
		assertThat(reference()).isNotEqualTo(new BankEntry(OTHER_TIME, "Avatar", 100, EINLAGERUNG));
	}

	@Test
	void differsByAvatar() {
		assertThat(reference()).isNotEqualTo(new BankEntry(TIME, "Other", 100, EINLAGERUNG));
	}

	@Test
	void differsByAmount() {
		assertThat(reference()).isNotEqualTo(new BankEntry(TIME, "Avatar", 999, EINLAGERUNG));
	}

	@Test
	void differsByType() {
		assertThat(reference()).isNotEqualTo(new BankEntry(TIME, "Avatar", 100, ENTNAHME));
	}

	private static BankEntry reference() {
		return new BankEntry(TIME, "Avatar", 100, EINLAGERUNG);
	}
}
