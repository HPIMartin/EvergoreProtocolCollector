package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarSummary(@JsonProperty("avatar") String avatar, @JsonProperty("bankWithdrawn") long bankWithdrawn, @JsonProperty("bankDeposited") long bankDeposited,
		@JsonProperty("storageWithdrawn") long storageWithdrawn, @JsonProperty("storageDeposited") long storageDeposited, @JsonProperty("net") long net,
		@JsonProperty("lastBankActivity") Instant lastBankActivity, @JsonProperty("lastStorageActivity") Instant lastStorageActivity) {}
