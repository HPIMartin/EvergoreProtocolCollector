package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.AdminReport;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.HealthFacts;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.RunReport;
import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;

import static dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList.UNREFRESHED_MEMBERS;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

public class RunReportSteps {
	private static final String STATUS = "Stand: ";
	private static final List<String> DATE_LABELS = List
			.of(STATUS, "Letzter Scrape: ", "Letzte Neuberechnung: ", "Letzter Scrape-Fehler: ", "Letzter Fehler bei der Neuberechnung: ");

	private final AdminStatusSteps admin;
	private final HealthReport health;

	public RunReportSteps(AdminStatusSteps admin, HealthReport health) {
		this.admin = admin;
		this.health = health;
	}

	@ParameterType(FindingList.NAMES)
	public FindingList findingList(String phrase) {
		return FindingList.named(phrase);
	}

	@ParameterType("\"[^\"]*\"(?:, \"[^\"]*\")*")
	public List<String> quotedNames(String names) {
		return Arrays.stream(names.substring(1, names.length() - 1).split("\", \"")).toList();
	}

	@Then("{surface} dates the last successful scrape and the last successful recompute at {moment}")
	public void datesTheLastSuccessfulScrapeAndRecompute(Surface surface, LocalDateTime moment) {
		RunReport report = reportOn(surface);

		assertThat(report.lastSuccessfulScrape()).contains(instantOf(moment));
		assertThat(report.lastSuccessfulRecompute()).contains(instantOf(moment));
	}

	@Then("{surface} dates the last successful scrape at {moment}")
	public void datesTheLastSuccessfulScrape(Surface surface, LocalDateTime moment) {
		assertThat(reportOn(surface).lastSuccessfulScrape()).contains(instantOf(moment));
	}

	@Then("{surface} dates the last successful recompute at {moment}")
	public void datesTheLastSuccessfulRecompute(Surface surface, LocalDateTime moment) {
		assertThat(reportOn(surface).lastSuccessfulRecompute()).contains(instantOf(moment));
	}

	@Then("{surface} dates the last scrape failure at {moment}")
	public void datesTheLastScrapeFailure(Surface surface, LocalDateTime moment) {
		assertThat(reportOn(surface).lastScrapeFailure()).contains(instantOf(moment));
	}

	@Then("{surface} dates the last recompute failure at {moment}")
	public void datesTheLastRecomputeFailure(Surface surface, LocalDateTime moment) {
		assertThat(reportOn(surface).lastRecomputeFailure()).contains(instantOf(moment));
	}

	@Then("{surface} dates no successful recompute")
	public void datesNoSuccessfulRecompute(Surface surface) {
		assertThat(reportOn(surface).lastSuccessfulRecompute()).isEmpty();
	}

	@Then("{surface} reports no failure")
	public void reportsNoFailure(Surface surface) {
		RunReport report = reportOn(surface);

		assertThat(report.lastScrapeFailure()).isEmpty();
		assertThat(report.lastRecomputeFailure()).isEmpty();
	}

	@Then("{surface} names {string} as a member whose figures could not be refreshed")
	public void namesAnUnrefreshedMember(Surface surface, String member) {
		assertThat(reportOn(surface).list(UNREFRESHED_MEMBERS)).hasValueSatisfying(names -> assertThat(names).contains(member));
	}

	@Then("{surface} names no finding")
	public void namesNoFinding(Surface surface) {
		RunReport report = reportOn(surface);

		assertThat(FindingList.values()).allSatisfy(list -> assertThat(report.list(list)).as(list.name()).isEmpty());
		switch (surface) {
			case ADMIN_PAGE -> {
				List<String> lines = admin.adminPage().messages();
				assertThat(lines).as("the admin page's status").anyMatch(line -> line.startsWith(STATUS));
				assertThat(lines).as("the admin page's labelled lines").filteredOn(line -> line.contains(": ")).allMatch(line -> DATE_LABELS.stream().anyMatch(line::startsWith));
			}
			case HEALTH_REPORT -> {
				assertThat(report.lastSuccessfulRecompute()).as("a recompute that succeeded").isPresent();
				assertThat(new HealthFacts(health).countedFindings()).isEmpty();
			}
		}
	}

	@Then("{surface} lists {findingList} in this order: {quotedNames}")
	public void listsInThisOrder(Surface surface, FindingList list, List<String> names) {
		assertThat(reportOn(surface).list(list)).contains(names);
	}

	private RunReport reportOn(Surface surface) {
		return switch (surface) {
			case ADMIN_PAGE -> new AdminReport(admin.adminPage());
			case HEALTH_REPORT -> new HealthFacts(health);
		};
	}

	private static Instant instantOf(LocalDateTime moment) {
		return moment.atZone(APP_ZONE).toInstant();
	}
}
