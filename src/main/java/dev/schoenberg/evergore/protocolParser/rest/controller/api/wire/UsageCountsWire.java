package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsageCountsWire(@JsonProperty("logins") int logins, @JsonProperty("pageLoads") int pageLoads, @JsonProperty("logouts") int logouts,
		@JsonProperty("daysUsed") int daysUsed, @JsonProperty("latestUse") Instant latestUse) {}
