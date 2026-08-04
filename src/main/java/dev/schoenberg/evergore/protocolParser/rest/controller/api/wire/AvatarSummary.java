package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarSummary(@JsonProperty("avatar") String avatar, @JsonProperty("withdrawn") long withdrawn, @JsonProperty("deposited") long deposited) {}
