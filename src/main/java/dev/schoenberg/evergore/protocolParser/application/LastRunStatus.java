package dev.schoenberg.evergore.protocolParser.application;

import java.time.*;
import java.util.*;

public class LastRunStatus {

	private volatile Instant lastSuccessfulScrapeInstant;
	private volatile Instant lastScrapeFailureInstant;
	private volatile Instant lastSuccessfulRecomputeInstant;
	private volatile Instant lastRecomputeFailureInstant;
	private volatile boolean recomputeHealthy = false;
	private volatile List<String> unknownItemNames = List.of();
	private volatile List<String> failedAvatarNames = List.of();

	public void recordSuccessfulScrape(Instant when) {
		lastSuccessfulScrapeInstant = when;
	}

	public Optional<Instant> lastSuccessfulScrape() {
		return Optional.ofNullable(lastSuccessfulScrapeInstant);
	}

	public void recordScrapeFailure(Instant when) {
		lastScrapeFailureInstant = when;
	}

	public Optional<Instant> lastScrapeFailure() {
		return Optional.ofNullable(lastScrapeFailureInstant);
	}

	public void recordSuccessfulRecompute(Instant when) {
		lastSuccessfulRecomputeInstant = when;
		recomputeHealthy = true;
	}

	public Optional<Instant> lastSuccessfulRecompute() {
		return Optional.ofNullable(lastSuccessfulRecomputeInstant);
	}

	public void recordRecomputeFailure(Instant when) {
		lastRecomputeFailureInstant = when;
		recomputeHealthy = false;
	}

	public Optional<Instant> lastRecomputeFailure() {
		return Optional.ofNullable(lastRecomputeFailureInstant);
	}

	public boolean recomputeHealthy() {
		return recomputeHealthy;
	}

	public void recordUnknownItems(List<String> names) {
		unknownItemNames = List.copyOf(names);
	}

	public List<String> unknownItemNames() {
		return unknownItemNames;
	}

	public void recordFailedAvatars(List<String> names) {
		failedAvatarNames = List.copyOf(names);
	}

	public List<String> failedAvatarNames() {
		return failedAvatarNames;
	}
}
