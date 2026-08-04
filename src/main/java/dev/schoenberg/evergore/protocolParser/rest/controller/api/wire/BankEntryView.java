package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankEntryView(@JsonProperty("timestamp") Instant timestamp, @JsonProperty("avatar") String avatar, @JsonProperty("amount") int amount,
		@JsonProperty("transferType") String transferType) {}
