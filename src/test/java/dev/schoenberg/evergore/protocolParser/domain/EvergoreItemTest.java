package dev.schoenberg.evergore.protocolParser.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.EDELSTEINE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.HANDWERKSMATERIAL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.JAGDBEUTEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.ROHSTOFFE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.EISENBARREN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.GRANIT;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KRISTALL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KUPFERERZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGIESPLITTER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGISCHE_AETHERBINDE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MARMOR;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.PFEILE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.SCHIEFER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STERNENSTAUB;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.UNDEFINED;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EvergoreItemTest {
	private static final List<EvergoreItem> RAW_STONES = List.of(MARMOR, GRANIT, SCHIEFER);
	private static final Set<Category> DONATED_CATEGORIES = Set.of(ROHSTOFFE, JAGDBEUTEN, EDELSTEINE);
	private static final Set<String> TRADER_GOODS = Set
			.of("Schmiedeöl", "Bogensalbe", "Harz", "Zwirn", "Steinkohle", "Nähgarn", "Lederfett", "Magiesplitter", "Federn", "Salz", "Mörtel", "Schleifstein", "Elbenhaar",
					"Wattierung", "Granitharz", "Glaszwirn", "Drachenzunder", "Schutzpolster", "Ledernieten", "Phasenkraut", "Pfeilharz", "Kristallat", "Edelmörtel", "Griffband",
					"Nieten", "Vulkandraht", "Beschläge", "Erdenblut", "Drachinschneiden");

	@Test
	void everyItemCostsSixtyPercentOfItsMarketValueWhenWithdrawn() {
		for (EvergoreItem item : EvergoreItem.values()) {
			double cost = item.getWithdrawlValue();

			assertThat(cost).as("withdrawl value of %s", item.name()).isCloseTo(item.marketValue * 0.6d, within(0.0001d));
		}
	}

	@Test
	void depositingMinedRawMaterialCreditsNothing() {
		double credited = KUPFERERZ.getStorageValue();

		assertThat(credited).isZero();
	}

	@Test
	void depositingHuntLootCreditsNothing() {
		double credited = STERNENSTAUB.getStorageValue();

		assertThat(credited).isZero();
	}

	@Test
	void depositingGemsCreditsNothing() {
		double credited = KRISTALL.getStorageValue();

		assertThat(credited).isZero();
	}

	@Test
	void depositingGoodsBoughtFromTheGuildTraderCreditsTheirFullMarketValue() {
		double credited = MAGIESPLITTER.getStorageValue();

		assertThat(credited).isCloseTo(60d, within(0.0001d));
	}

	@Test
	void depositingCraftedGoodsCreditsSixtyPercentOfTheirMarketValue() {
		double credited = MAGISCHE_AETHERBINDE.getStorageValue();

		assertThat(credited).isCloseTo(154.2d, within(0.0001d));
	}

	@Test
	void depositingProcessedRawMaterialCreditsSixtyPercentOfItsMarketValue() {
		double credited = EISENBARREN.getStorageValue();

		assertThat(credited).isCloseTo(72d, within(0.0001d));
	}

	@Test
	void everyCategorySitsInTheCreditTierItsOriginPutsItIn() {
		for (Category category : Category.values()) {
			double placement = category.placement;

			assertThat(placement).as("placement of %s", category.name()).isEqualTo(expectedPlacementOf(category));
		}
	}

	@Test
	void onlyTheGoodsBoughtFromTheGuildTraderAreCreditedInFull() {
		Set<String> creditedInFull = stream(EvergoreItem.values()).filter(item -> item.category.placement == 1d).map(item -> item.ingameName).collect(toSet());

		assertThat(creditedInFull).isEqualTo(TRADER_GOODS);
	}

	@Test
	void theUnknownItemFallbackCannotEarnACreditOfItsOwn() {
		double placement = UNDEFINED.category.placement;

		assertThat(placement).isZero();
	}

	@Test
	void theGuildsArrowExampleGainsNinetySixGold() {
		double gain = valueGainOfCrafting(PFEILE);

		assertThat(gain).isCloseTo(96d, within(0.0001d));
	}

	@Test
	void theGuildsArrowExampleTakesItsIngredientsFromTheStorageForOneHundredFortySeven() {
		double ingredientCost = withdrawlCostOfIngredientsOf(PFEILE);

		assertThat(ingredientCost).isCloseTo(147d, within(0.0001d));
	}

	@Test
	void smeltingIronBarsGainsOneHundredTwentyGold() {
		double gain = valueGainOfCrafting(EISENBARREN);

		assertThat(gain).isCloseTo(120d, within(0.0001d));
	}

	@Test
	void whatIngredientsCostDoesNotDependOnTheCreditTierTheySitIn() {
		double ingredientCost = withdrawlCostOfIngredientsOf(EISENBARREN);

		assertThat(ingredientCost).isCloseTo(0.6d * ingredientMarketValueOf(EISENBARREN), within(0.0001d));
	}

	@Test
	void aCraftingGainIsSixtyPercentOfTheMarginBetweenProductAndIngredients() {
		double gain = valueGainOfCrafting(EISENBARREN);

		assertThat(gain).isCloseTo(0.6d * (EISENBARREN.marketValue * EISENBARREN.recipe.amount - ingredientMarketValueOf(EISENBARREN)), within(0.0001d));
	}

	@Test
	void takingACraftedGoodOutAndPuttingItBackIsNeutral() {
		double roundTrip = MAGISCHE_AETHERBINDE.getStorageValue() - MAGISCHE_AETHERBINDE.getWithdrawlValue();

		assertThat(roundTrip).isZero();
	}

	@Test
	void takingHuntLootOutAndPuttingItBackCostsWhatTheWithdrawlCost() {
		double roundTrip = STERNENSTAUB.getStorageValue() - STERNENSTAUB.getWithdrawlValue();

		assertThat(roundTrip).isCloseTo(-72d, within(0.0001d));
	}

	@Test
	void takingTraderGoodsOutAndPuttingThemBackEarnsFortyPercentOfTheirMarketValue() {
		double roundTrip = MAGIESPLITTER.getStorageValue() - MAGIESPLITTER.getWithdrawlValue();

		assertThat(roundTrip).isCloseTo(24d, within(0.0001d));
	}

	@Test
	void noTwoCatalogEntriesShareAnIngameName() {
		Map<String, List<String>> constantsByIngameName = stream(EvergoreItem.values()).collect(groupingBy(item -> item.ingameName, mapping(EvergoreItem::name, toList())));

		List<List<String>> entriesSharingAnIngameName = constantsByIngameName.values().stream().filter(constants -> constants.size() > 1).toList();

		assertThat(entriesSharingAnIngameName).isEmpty();
	}

	@Test
	void theRawStonesCarryTheNameTheGameUses() {
		List<String> names = RAW_STONES.stream().map(item -> item.ingameName).toList();

		assertThat(names).containsExactly("Marmor", "Granit", "Schiefer");
	}

	@Test
	void theRawStonesKeepThePriceTheWikiTableRecords() {
		List<Integer> marketValues = RAW_STONES.stream().map(item -> item.marketValue).toList();

		assertThat(marketValues).containsExactly(120, 90, 60);
	}

	private static double valueGainOfCrafting(EvergoreItem product) {
		return product.getStorageValue() * product.recipe.amount - withdrawlCostOfIngredientsOf(product);
	}

	private static double withdrawlCostOfIngredientsOf(EvergoreItem product) {
		return product.recipe.ingredients.stream().mapToDouble(ingredient -> ingredient.amount * ingredient.item.getWithdrawlValue()).sum();
	}

	private static double ingredientMarketValueOf(EvergoreItem product) {
		return product.recipe.ingredients.stream().mapToDouble(ingredient -> ingredient.amount * ingredient.item.marketValue).sum();
	}

	private static double expectedPlacementOf(Category category) {
		if (DONATED_CATEGORIES.contains(category)) {
			return 0d;
		}
		return category == HANDWERKSMATERIAL ? 1d : 0.6d;
	}
}
