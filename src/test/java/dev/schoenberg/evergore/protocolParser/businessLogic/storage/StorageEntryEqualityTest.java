package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;

class StorageEntryEqualityTest {

	private static final Instant TIME = Instant.parse("2001-12-11T13:37:00Z");
	private static final Instant OTHER_TIME = Instant.parse("2002-01-01T00:00:00Z");

	@Test
	void equalWhenAllFieldsMatch() {
		assertThat(reference()).isEqualTo(reference()).hasSameHashCodeAs(reference());
	}

	@Test
	void differsByTimestamp() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(OTHER_TIME, "Avatar", 5, "Drachenhaut", 80, EINLAGERUNG));
	}

	@Test
	void differsByAvatar() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(TIME, "Other", 5, "Drachenhaut", 80, EINLAGERUNG));
	}

	@Test
	void differsByQuantity() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(TIME, "Avatar", 9, "Drachenhaut", 80, EINLAGERUNG));
	}

	@Test
	void differsByName() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(TIME, "Avatar", 5, "Leinentuch", 80, EINLAGERUNG));
	}

	@Test
	void differsByQuality() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(TIME, "Avatar", 5, "Drachenhaut", 99, EINLAGERUNG));
	}

	@Test
	void differsByType() {
		assertThat(reference()).isNotEqualTo(new StorageEntry(TIME, "Avatar", 5, "Drachenhaut", 80, ENTNAHME));
	}

	private static StorageEntry reference() {
		return new StorageEntry(TIME, "Avatar", 5, "Drachenhaut", 80, EINLAGERUNG);
	}
}
