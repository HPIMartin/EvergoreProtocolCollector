package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountWire(@JsonProperty("name") String name, @JsonProperty("rests") boolean rests, @JsonProperty("lastLogin") Instant lastLogin) {}
