package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarSummary(@JsonProperty("avatar") String avatar, @JsonProperty("bankWithdrawn") Long bankWithdrawn, @JsonProperty("bankDeposited") Long bankDeposited,
		@JsonProperty("storageWithdrawn") Long storageWithdrawn, @JsonProperty("storageDeposited") Long storageDeposited, @JsonProperty("net") Long net,
		@JsonProperty("donation") Long donation, @JsonProperty("craftSubsidy") Long craftSubsidy, @JsonProperty("balance") Long balance,
		@JsonProperty("lastBankActivity") Instant lastBankActivity, @JsonProperty("lastStorageActivity") Instant lastStorageActivity,
		@JsonProperty("staleSumsFrom") Instant staleSumsFrom) {}
