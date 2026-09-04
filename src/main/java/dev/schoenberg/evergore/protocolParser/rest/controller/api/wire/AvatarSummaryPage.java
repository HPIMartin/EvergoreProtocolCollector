package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarSummaryPage(@JsonProperty("page") int page, @JsonProperty("size") int size, @JsonProperty("totalCount") long totalCount,
		@JsonProperty("totals") GuildTotals totals, @JsonProperty("items") List<AvatarSummary> items) {}
