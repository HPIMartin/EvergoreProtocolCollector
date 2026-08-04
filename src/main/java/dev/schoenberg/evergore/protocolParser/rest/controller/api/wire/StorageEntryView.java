package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageEntryView(@JsonProperty("timestamp") Instant timestamp, @JsonProperty("avatar") String avatar, @JsonProperty("quantity") int quantity,
		@JsonProperty("name") String name, @JsonProperty("quality") int quality, @JsonProperty("transferType") String transferType) {}
