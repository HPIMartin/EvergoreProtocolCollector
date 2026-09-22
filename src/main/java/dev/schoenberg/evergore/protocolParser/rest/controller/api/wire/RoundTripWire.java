package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoundTripWire(@JsonProperty("avatar") String avatar, @JsonProperty("item") String item, @JsonProperty("quantity") int quantity) {}
