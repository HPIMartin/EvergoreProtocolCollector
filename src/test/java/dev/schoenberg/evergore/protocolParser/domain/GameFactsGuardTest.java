package dev.schoenberg.evergore.protocolParser.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Ingredient;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.NOT_CRAFTABLE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.UNKNOWN_RECIPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;

class GameFactsGuardTest {
	private static final String RECORDED_FACTS = "/gameCatalog/game-facts.tsv";
	private static final String HEADER = "kind\titem\tvalue\tingredients\tsource\tread";
	private static final Pattern OWNERSHIP_MARKER = Pattern.compile("Lagerzugriff|Hergestellt von|Kein Besitzer");

	@Test
	void everyPriceTheGameGaveIsThePriceTheCatalogHolds() {
		List<String> divergences = new ArrayList<>();
		for (Fact fact : factsOfKind("price")) {
			CatalogLookup.itemFor(fact.item()).ifPresent(entry -> {
				if (entry.marketValue != Integer.parseInt(fact.value())) {
					divergences.add(fact.item() + ": game " + fact.value() + ", catalog " + entry.marketValue + " (" + fact.source() + ", read " + fact.read() + ")");
				}
			});
		}

		assertThat(divergences).isEmpty();
	}

	@Test
	void everyItemTheGamePricesHasACatalogEntryToPrice() {
		List<String> uncatalogued = factsOfKind("price").stream().map(Fact::item).filter(item -> CatalogLookup.itemFor(item).isEmpty()).sorted().toList();

		assertThat(uncatalogued).isEmpty();
	}

	@Test
	void everyRecipeTheGameShowsIsTheRecipeTheCatalogHolds() {
		List<String> divergences = new ArrayList<>();
		for (Fact fact : factsOfKind("recipe")) {
			CatalogLookup.itemFor(fact.item()).ifPresent(entry -> {
				String recorded = fact.value() + " <- " + fact.ingredients();
				String catalogued = describe(entry.recipe);
				if (!recorded.equals(catalogued)) {
					divergences.add(fact.item() + ": game " + recorded + ", catalog " + catalogued + " (" + fact.source() + ", read " + fact.read() + ")");
				}
			});
		}

		assertThat(divergences).isEmpty();
	}

	@Test
	void theGuardHoldsTheCatalogToEveryFactItCanAndNamesWhatItCannot() {
		List<String> comparedPrices = factsOfKind("price").stream().map(Fact::item).filter(item -> CatalogLookup.itemFor(item).isPresent()).toList();
		List<String> comparedRecipes = factsOfKind("recipe").stream().map(Fact::item).filter(item -> CatalogLookup.itemFor(item).isPresent()).toList();
		List<String> recipesForItemsTheCatalogDoesNotHold = factsOfKind("recipe").stream().map(Fact::item).filter(item -> CatalogLookup.itemFor(item).isEmpty()).sorted().toList();

		assertThat(comparedPrices).hasSize(671);
		assertThat(comparedRecipes).hasSize(424);
		assertThat(recipesForItemsTheCatalogDoesNotHold).isEmpty();
	}

	@Test
	void theCompletePagedReadPricesTheCatalogEntriesTheHeadlineClaims() {
		assertThat(entriesPricedBy("stock_out")).hasSize(433);
		assertThat(entriesPricedBy("market_all_articles")).hasSize(229);
		assertThat(entriesPricedBy("")).hasSize(483);
		assertThat(EvergoreItem.values()).hasSize(601);
	}

	@Test
	void everyRecordedFactSaysWhichPageItWasReadFromAndWhen() {
		List<String> malformed = new ArrayList<>();
		for (Fact fact : recordedFacts()) {
			if (fact.source().isBlank() || !isADate(fact.read())) {
				malformed.add(fact.item() + " (" + fact.source() + ", read " + fact.read() + ")");
			}
		}

		assertThat(malformed).isEmpty();
	}

	@Test
	void theRecordedFactsNameNoGuildMemberAndNoStorageAccess() {
		assertThat(OWNERSHIP_MARKER.matcher(rawFacts()).find()).isFalse();
	}

	@Test
	void theRecordedFactsCarryTheColumnsTheGuardReads() {
		assertThat(rawFacts().lines().findFirst()).contains(HEADER);
	}

	private static Set<EvergoreItem> entriesPricedBy(String sourcePrefix) {
		return factsOfKind("price")
				.stream()
				.filter(fact -> fact.source().startsWith(sourcePrefix))
				.map(fact -> CatalogLookup.itemFor(fact.item()))
				.flatMap(Optional::stream)
				.collect(toCollection(TreeSet::new));
	}

	private static String describe(Recipe recipe) {
		if (recipe == NOT_CRAFTABLE) {
			return "NOT_CRAFTABLE";
		}
		if (recipe == UNKNOWN_RECIPE) {
			return "UNKNOWN_RECIPE";
		}
		return recipe.amount + " <- " + recipe.ingredients.stream().map(GameFactsGuardTest::describe).reduce((a, b) -> a + " + " + b).orElse("");
	}

	private static String describe(Ingredient ingredient) {
		return ingredient.amount + " " + ingredient.item.ingameName;
	}

	private static boolean isADate(String candidate) {
		try {
			LocalDate.parse(candidate);
			return true;
		} catch (RuntimeException notADate) {
			return false;
		}
	}

	private static List<Fact> factsOfKind(String kind) {
		return recordedFacts().stream().filter(fact -> fact.kind().equals(kind)).toList();
	}

	private static List<Fact> recordedFacts() {
		try (Stream<String> lines = rawFacts().lines()) {
			return lines.skip(1).map(GameFactsGuardTest::parse).toList();
		}
	}

	private static Fact parse(String line) {
		String[] columns = line.split("\t", -1);
		assertThat(columns).as("a recorded fact carries the guard's six columns, this row carries %s: %s", columns.length, line).hasSize(6);
		return new Fact(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5]);
	}

	private static String rawFacts() {
		try (InputStream evidence = GameFactsGuardTest.class.getResourceAsStream(RECORDED_FACTS)) {
			assertThat(evidence).as("%s carries what the game said, and the catalog cannot be held to anything without it", RECORDED_FACTS).isNotNull();
			return new String(evidence.readAllBytes(), UTF_8);
		} catch (IOException theEvidenceIsNotThere) {
			throw new UncheckedIOException(theEvidenceIsNotThere);
		}
	}

	private record Fact(String kind, String item, String value, String ingredients, String source, String read) {}
}
