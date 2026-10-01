package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.domain.CatalogLookup;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.AEXTE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.AEXTE_2H;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.BANDAGEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.BOEGEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.EDELSTEINE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.HANDWERKSMATERIAL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.JAGDBEUTEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.KEULEN_2H;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.MUNITION_ARMBRUESTE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.MUNITION_BOEGEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.MUNITION_MAGIESTAEBE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.ROHSTOFFE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.SCHWERTER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.STANGENWAFFEN_2H;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.VERARBEITETE_ROHSTOFFE;
import static org.assertj.core.api.Assertions.assertThat;

public class ValuationSteps {
	private static final Map<String, Category> KINDS = Map
			.ofEntries(Map.entry("Rohstoffe", ROHSTOFFE), Map.entry("Jagdbeuten", JAGDBEUTEN), Map.entry("Edelsteine", EDELSTEINE),
					Map.entry("Handwerksmaterial", HANDWERKSMATERIAL), Map.entry("verarbeitete Rohstoffe", VERARBEITETE_ROHSTOFFE), Map.entry("Bandagen", BANDAGEN),
					Map.entry("Munition Bögen", MUNITION_BOEGEN), Map.entry("Munition Armbrüste", MUNITION_ARMBRUESTE), Map.entry("Munition Magiestäbe", MUNITION_MAGIESTAEBE),
					Map.entry("Bögen", BOEGEN), Map.entry("Äxte", AEXTE), Map.entry("Äxte [2H]", AEXTE_2H), Map.entry("Stangenwaffen [2H]", STANGENWAFFEN_2H),
					Map.entry("Schwerter", SCHWERTER), Map.entry("Keulen [2H]", KEULEN_2H));
	private static final String UNKNOWN_ITEMS_PREFIX = "Unbekannte Items";

	private final MemberBrowser browser;

	public ValuationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@Given("the price list the service ships values:")
	public void thePriceListTheServiceShipsValues(DataTable table) {
		for (Map<String, String> row : table.asMaps()) {
			String name = row.get("item");
			EvergoreItem item = CatalogLookup.itemFor(name).orElseThrow(() -> new AssertionError("The price list carries no item named " + name));

			assertThat(item.category).as("the category the price list ships " + name + " under").isEqualTo(kindNamed(row.get("kind")));
			assertThat(item.marketValue).as("the market value the price list ships " + name + " at").isEqualTo(Integer.parseInt(row.get("market value")));
		}
	}

	@Then("the admin page lists no unknown item")
	public void theAdminPageListsNoUnknownItem() {
		AdminPage page = browser.read("read-admin.js", AdminPage.class);

		assertThat(page.messages()).noneMatch(message -> message.startsWith(UNKNOWN_ITEMS_PREFIX));
	}

	private static Category kindNamed(String kind) {
		Category category = KINDS.get(kind);
		if (category == null) {
			throw new AssertionError("No category is known for the kind \"" + kind + "\"");
		}
		return category;
	}
}
