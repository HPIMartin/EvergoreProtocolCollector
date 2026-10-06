package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GuildTotals(@JsonProperty("bankWithdrawn") Long bankWithdrawn, @JsonProperty("bankDeposited") Long bankDeposited,
		@JsonProperty("storageWithdrawn") Long storageWithdrawn, @JsonProperty("storageDeposited") Long storageDeposited, @JsonProperty("net") Long net,
		@JsonProperty("donation") Long donation, @JsonProperty("craftSubsidy") Long craftSubsidy, @JsonProperty("balance") Long balance,
		@JsonProperty("storageValue") Long storageValue, @JsonProperty("containsStaleSums") boolean containsStaleSums) {}
