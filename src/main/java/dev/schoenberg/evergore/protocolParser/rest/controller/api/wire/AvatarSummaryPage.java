package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarSummaryPage(@JsonProperty("lastUpdated") Instant lastUpdated, @JsonProperty("page") int page, @JsonProperty("size") int size,
		@JsonProperty("totalCount") long totalCount, @JsonProperty("items") List<AvatarSummary> items) {}
