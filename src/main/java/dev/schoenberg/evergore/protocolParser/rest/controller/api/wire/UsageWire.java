package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsageWire(@JsonProperty("lastThirtyDays") UsageWindowWire lastThirtyDays, @JsonProperty("allTheTime") UsageWindowWire allTheTime,
		@JsonProperty("recordedSince") LocalDate recordedSince, @JsonProperty("lostSinceStart") long lostSinceStart) {}
