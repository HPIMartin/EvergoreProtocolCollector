package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminStatus(@JsonProperty("lastUpdated") Instant lastUpdated, @JsonProperty("lastSuccessfulScrape") Instant lastSuccessfulScrape,
		@JsonProperty("lastScrapeFailure") Instant lastScrapeFailure, @JsonProperty("lastSuccessfulRecompute") Instant lastSuccessfulRecompute,
		@JsonProperty("lastRecomputeFailure") Instant lastRecomputeFailure, @JsonProperty("unknownItemNames") List<String> unknownItemNames,
		@JsonProperty("failedAvatarNames") List<String> failedAvatarNames) {}
