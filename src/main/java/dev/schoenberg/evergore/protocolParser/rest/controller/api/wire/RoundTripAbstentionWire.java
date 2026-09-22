package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoundTripAbstentionWire(@JsonProperty("avatar") String avatar, @JsonProperty("item") String item) {}
