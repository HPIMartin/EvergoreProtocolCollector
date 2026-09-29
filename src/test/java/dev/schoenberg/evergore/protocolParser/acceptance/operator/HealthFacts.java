package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;

public record HealthFacts(HealthReport report) implements RunReport {
	public List<String> countedFindings() {
		return report.lastRunDetails().propertyStream().map(Map.Entry::getKey).filter(name -> name.endsWith("Count")).toList();
	}

	public Optional<Integer> count(String detail) {
		return present(detail).map(JsonNode::asInt);
	}

	@Override
	public Optional<Instant> lastSuccessfulScrape() {
		return moment("lastSuccessfulScrape");
	}

	@Override
	public Optional<Instant> lastSuccessfulRecompute() {
		return moment("lastSuccessfulRecompute");
	}

	@Override
	public Optional<Instant> lastScrapeFailure() {
		return moment("lastScrapeFailure");
	}

	@Override
	public Optional<Instant> lastRecomputeFailure() {
		return moment("lastRecomputeFailure");
	}

	@Override
	public Optional<List<String>> list(FindingList list) {
		return present(list.detail()).map(names -> names.valueStream().map(JsonNode::asText).toList());
	}

	private Optional<Instant> moment(String detail) {
		return present(detail).map(moment -> Instant.parse(moment.asText()));
	}

	private Optional<JsonNode> present(String detail) {
		JsonNode node = report.lastRunDetails().path(detail);
		return node.isMissingNode() ? Optional.empty() : Optional.of(node);
	}
}
