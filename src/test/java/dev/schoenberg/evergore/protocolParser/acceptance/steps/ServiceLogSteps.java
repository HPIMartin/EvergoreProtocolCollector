package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceLogSteps {
	private static final String DROPPED_ENTRY = "Dropping protocol entry: ";
	private static final String SKIPPED_ITEM_LINE = "Skipping item line with an unparseable number: ";
	private static final Pattern HEADLINE = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4} ");
	private static final String NOTHING_READ = "Protocol entry yielded no parseable items: ";

	private final ServiceLog log;
	private final GameProtocols protocols;

	public ServiceLogSteps(ServiceLog log, GameProtocols protocols) {
		this.log = log;
		this.protocols = protocols;
	}

	@Then("the operator finds the skipped headline {string} named in the service's log")
	public void theSkippedHeadlineIsNamed(String headline) {
		assertThat(log.lines()).anyMatch(line -> line.startsWith(DROPPED_ENTRY) && line.endsWith(": " + headline));
	}

	@Then("the operator finds the skipped item line {string} named in the service's log")
	public void theSkippedItemLineIsNamed(String itemLine) {
		assertThat(log.lines()).contains(SKIPPED_ITEM_LINE + itemLine);
	}

	@Then("the operator finds no skipped item line named in the service's log")
	public void noSkippedItemLineIsNamed() {
		PageContents shown = protocols.pageContents();
		List<String> protocolLines = Stream.of(shown.lager(), shown.bank()).flatMap(List::stream).filter(line -> !line.isBlank() && !HEADLINE.matcher(line).find()).toList();

		assertThat(log.lines()).noneMatch(line -> line.startsWith(SKIPPED_ITEM_LINE));
		assertThat(log.lines()).noneMatch(line -> protocolLines.stream().anyMatch(line::contains));
	}

	@Then("the operator finds the entry {string} named in the service's log as yielding nothing it could read")
	public void theEntryIsNamedAsYieldingNothing(String headline) {
		assertThat(log.lines()).contains(NOTHING_READ + headline);
	}

	@Then("the operator finds a line naming {string} and {string} in the service's log")
	public void aLineNames(String first, String second) {
		assertThat(log.lines()).anyMatch(line -> line.contains(first) && line.contains(second));
	}

	@Then("the service's log says {string}")
	public void theServicesLogSays(String line) {
		assertThat(log.lines()).contains(line);
	}
}
