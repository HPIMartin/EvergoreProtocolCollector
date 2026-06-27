package dev.schoenberg.evergore.protocolParser.businessLogic.base;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;

class TransferTypeTest {

	@Test
	void placementMapsToGermanDepositString() {
		assertThat(EINLAGERUNG.toGermanString()).isEqualTo("Einlagerung");
	}

	@Test
	void withdrawalMapsToGermanWithdrawalString() {
		assertThat(ENTNAHME.toGermanString()).isEqualTo("Entnahme");
	}
}
