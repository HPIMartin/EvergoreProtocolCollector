package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsageWindowWire(@JsonProperty("accounts") List<AccountUsageWire> accounts, @JsonProperty("guild") UsageCountsWire guild,
		@JsonProperty("adminKey") UsageCountsWire adminKey) {}
