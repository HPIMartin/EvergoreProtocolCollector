package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountUsageWire(@JsonProperty("name") String name, @JsonProperty("counts") UsageCountsWire counts) {}
