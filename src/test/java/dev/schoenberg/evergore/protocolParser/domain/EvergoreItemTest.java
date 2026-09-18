package dev.schoenberg.evergore.protocolParser.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.EDELSTEINE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.HANDWERKSMATERIAL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.JAGDBEUTEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.LEICHTE_RUESTUNG_LEDER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.LEICHTE_RUESTUNG_STOFF;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.LEICHTE_SCHILDE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.ROHSTOFFE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.SCHWERER_SCHILDE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.SCHWERE_RUESTUNG_METALL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.EISENBARREN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.GRANIT;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.JAGDPFEILE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KRISTALL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KUPFERERZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGIESPLITTER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGISCHE_AETHERBINDE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MARMOR;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.OBSIDIAN_PIKE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.PANZERBRECHER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.PFEILE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.NOT_CRAFTABLE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.SCHIEFER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.SMARAGD_PIKE_2H;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STEINBRECHER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STERNENSTAUB;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.UEBUNGSSTUECK_KUPFERSCHWERT;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.UNDEFINED;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EvergoreItemTest {
	private static final String PRACTICE_PIECE = "Übungsstück-";
	private static final Pattern GEM_PREFIX = Pattern.compile("^(Achat|Diamant|Jade|Jaspis|Kristall|Lapis|Obsidian|Onyx|Perlmutt|Pyrit|Quarz|Rubin|Saphir|Smaragd|Topas)-");
	private static final Set<Category> ARMOUR = Set.of(LEICHTE_RUESTUNG_LEDER, LEICHTE_RUESTUNG_STOFF, SCHWERE_RUESTUNG_METALL, LEICHTE_SCHILDE, SCHWERER_SCHILDE);
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
	void pricesEveryAmmunitionTypeAtTheGoldTheGuildStorageListsPerPiece() {
		Map<String, Integer> pricePerAmmunition = new TreeMap<>();
		for (EvergoreItem ammunition : List.of(STEINBRECHER, JAGDPFEILE, PANZERBRECHER)) {
			pricePerAmmunition.put(ammunition.ingameName, ammunition.marketValue);
		}

		assertThat(pricePerAmmunition).containsExactlyInAnyOrderEntriesOf(Map.of("Steinbrecher", 44, "Jagdpfeile", 5, "Panzerbrecher", 11));
	}

	@Test
	void onlyDeliberatelyWorthlessGearIsCataloguedAtZero() {
		List<String> worthless = stream(EvergoreItem.values()).filter(item -> item.marketValue == 0).map(item -> item.ingameName).toList();

		assertThat(worthless).hasSize(60);
		assertThat(worthless).allMatch(name -> name.equals("undefined") || name.startsWith(PRACTICE_PIECE) || name.startsWith("Mystisch"));
	}

	@Test
	void aPracticePieceCostsTheApprenticeExactlyWhatWithdrawingItsMaterialCharges() {
		double valueGain = valueGainOfCrafting(UEBUNGSSTUECK_KUPFERSCHWERT);

		assertThat(valueGain).isCloseTo(-120d, within(0.0001d));
	}

	@Test
	void everyPracticePieceYieldsASinglePiece() {
		List<String> yieldingMoreThanOne = practicePieces().filter(item -> item.recipe.amount != 1).map(item -> item.ingameName).toList();

		assertThat(yieldingMoreThanOne).isEmpty();
	}

	@Test
	void everyPracticePieceIsTrainedOnMaterialItsOwnCraftAlsoUses() {
		List<String> trainedOnForeignMaterial = practicePieces()
				.flatMap(piece -> piece.recipe.ingredients
						.stream()
						.filter(ingredient -> !materialUsedBy(piece.category).contains(ingredient.item))
						.map(ingredient -> piece.ingameName + " consumes " + ingredient.item.ingameName))
				.toList();

		assertThat(trainedOnForeignMaterial).isEmpty();
	}

	@Test
	void aCatalogEntryAnswersItsIngameNameAndEverySecondSpellingItCarries() {
		assertThat(OBSIDIAN_PIKE.allNames()).containsExactly("Obsidian-Pike", "Obsidian-Pike [2H]");
		assertThat(SMARAGD_PIKE_2H.allNames()).containsExactly("Smaragd-Pike [2H]", "Smaragd-Pike");
		assertThat(PFEILE.allNames()).containsExactly("Pfeile");
	}

	@Test
	void everyGemForgedWeaponOfOneGemAndOneCategoryCarriesOneAndTheSamePrice() {
		Map<String, Set<Integer>> pricesPerGroup = new TreeMap<>();
		for (EvergoreItem item : EvergoreItem.values()) {
			if (GEM_PREFIX.matcher(item.ingameName).find() && !ARMOUR.contains(item.category)) {
				pricesPerGroup
						.computeIfAbsent(GEM_PREFIX.matcher(item.ingameName).results().findFirst().orElseThrow().group(1) + "/" + item.category, key -> new TreeSet<>())
						.add(item.marketValue);
			}
		}

		assertThat(pricesPerGroup).hasSize(58);
		assertThat(pricesPerGroup).allSatisfy((group, prices) -> assertThat(prices).as(group).hasSize(1));
	}

	@Test
	void onlyTheDeliberatelyWorthlessAreCraftableForNothing() {
		List<String> craftableAndFree = stream(EvergoreItem.values())
				.filter(item -> !item.recipe.ingredients.isEmpty() && item.marketValue == 0)
				.filter(item -> !isDeliberatelyWorthless(item))
				.map(item -> item.ingameName)
				.toList();

		assertThat(craftableAndFree).isEmpty();
	}

	@Test
	void noItemWhoseValueReflectsItsInputsIsWorthLessThanTheIngredientsItsRecipeConsumes() {
		List<String> pricedBelowTheirIngredients = stream(EvergoreItem.values())
				.filter(item -> !item.recipe.ingredients.isEmpty())
				.filter(item -> !isDeliberatelyWorthless(item))
				.filter(item -> item.marketValue * item.recipe.amount < ingredientMarketValueOf(item))
				.map(item -> item.ingameName)
				.toList();

		assertThat(pricedBelowTheirIngredients).isEmpty();
	}

	@Test
	void aCatalogueEntryIsEitherNotCraftableOrNamesWhatItConsumes() {
		List<String> craftableFromNothing = stream(EvergoreItem.values())
				.filter(item -> item.recipe != NOT_CRAFTABLE && item.recipe.ingredients.isEmpty())
				.map(item -> item.ingameName)
				.toList();

		assertThat(craftableFromNothing).isEmpty();
	}

	@Test
	void nothingTheValueGuardsExcludeCarriesAPriceOfItsOwn() {
		List<String> excludedButPriced = stream(EvergoreItem.values())
				.filter(EvergoreItemTest::isDeliberatelyWorthless)
				.filter(item -> item.marketValue != 0)
				.map(item -> item.ingameName)
				.toList();

		assertThat(excludedButPriced).isEmpty();
	}

	@Test
	void everyPracticePieceNamesTheMaterialItIsTrainedOn() {
		List<String> withoutIngredients = practicePieces().filter(item -> item.recipe.ingredients.isEmpty()).map(item -> item.ingameName).toList();

		assertThat(withoutIngredients).isEmpty();
	}

	@Test
	void theCatalogStillHoldsEveryProductionChainItRecorded() {
		long craftables = stream(EvergoreItem.values()).filter(item -> !item.recipe.ingredients.isEmpty()).count();

		assertThat(craftables).isEqualTo(424);
	}

	@Test
	void everyCraftableIsCreditedAtLeastWhatItsWithdrawalCharges() {
		List<String> creditedBelowTheirCost = stream(EvergoreItem.values())
				.filter(item -> !item.recipe.ingredients.isEmpty() && item.category.placement < item.category.withdrawl)
				.map(item -> item.ingameName)
				.toList();

		assertThat(creditedBelowTheirCost).isEmpty();
	}

	@Test
	void onlyGatheredGoodsSitInACategoryThatCreditsADepositNothing() {
		List<String> craftedButUncredited = stream(EvergoreItem.values())
				.filter(item -> item.category.placement == 0d && !item.recipe.ingredients.isEmpty())
				.map(item -> item.ingameName)
				.toList();

		assertThat(craftedButUncredited).isEmpty();
	}

	@Test
	void noTwoCatalogEntriesClaimTheSameName() {
		List<String> everyName = stream(EvergoreItem.values()).flatMap(item -> item.allNames().stream()).toList();

		assertThat(everyName).doesNotHaveDuplicates();
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

	private static Stream<EvergoreItem> practicePieces() {
		return stream(EvergoreItem.values()).filter(item -> item.ingameName.startsWith(PRACTICE_PIECE));
	}

	private static Set<EvergoreItem> materialUsedBy(Category craft) {
		return stream(EvergoreItem.values())
				.filter(item -> item.category == craft && !item.ingameName.startsWith(PRACTICE_PIECE))
				.flatMap(item -> item.recipe.ingredients.stream())
				.map(ingredient -> ingredient.item)
				.collect(toSet());
	}

	private static boolean isDeliberatelyWorthless(EvergoreItem item) {
		return item.ingameName.startsWith(PRACTICE_PIECE) || item.ingameName.startsWith("Mystisch");
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
