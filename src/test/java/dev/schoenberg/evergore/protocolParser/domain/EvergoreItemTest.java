package dev.schoenberg.evergore.protocolParser.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvergoreItemTest {
	@Test
	void calculatesStorageAndWithdrawlForNotCraftableItems() {
		assertThat(EvergoreItem.KUPFERERZ.getStorageValue()).isZero();
		assertThat(EvergoreItem.KUPFERERZ.getWithdrawlValue()).isEqualTo(12);
	}

	@Test
	void calculatesStorageAndWithdrawlForCraftableItems() {
		assertThat(EvergoreItem.MAGISCHE_AETHERBINDE.getStorageValue()).isEqualTo(92.52d);
		assertThat(EvergoreItem.MAGISCHE_AETHERBINDE.getWithdrawlValue()).isEqualTo(154.2d);
	}

	@Test
	void calculatesStorageAndWithdrawlForGems() {
		assertThat(EvergoreItem.KRISTALL.getStorageValue()).isZero();
		assertThat(EvergoreItem.KRISTALL.getWithdrawlValue()).isEqualTo(300);
	}
}
