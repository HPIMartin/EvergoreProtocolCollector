package dev.schoenberg.evergore.protocolParser.domain;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.NOT_CRAFTABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

	@Test
	void withdrawlValueIsMarketValueScaledBy0Point6ForEveryItem() {
		for (EvergoreItem item : EvergoreItem.values()) {
			double expected = item.marketValue * 0.6d;
			assertThat(item.getWithdrawlValue()).as("withdrawl value for %s", item.name()).isCloseTo(expected, within(0.0001d));
		}
	}

	@Test
	void storageValueIsZeroForAllNotCraftableItems() {
		for (EvergoreItem item : EvergoreItem.values()) {
			if (item.recipe == NOT_CRAFTABLE) {
				assertThat(item.getStorageValue()).as("storage value for NOT_CRAFTABLE item %s", item.name()).isZero();
			}
		}
	}

	@Test
	void storageValueIsSumOfIngredientWithdrawlCostsPerUnitForCraftableItems() {
		for (EvergoreItem item : EvergoreItem.values()) {
			if (item.recipe != NOT_CRAFTABLE) {
				double expectedRecipeTotal = item.recipe.ingredients.stream().mapToDouble(ing -> ing.amount * ing.item.getWithdrawlValue()).sum();
				double expected = expectedRecipeTotal / item.recipe.amount;
				assertThat(item.getStorageValue()).as("storage value for craftable item %s", item.name()).isCloseTo(expected, within(0.0001d));
			}
		}
	}
}
