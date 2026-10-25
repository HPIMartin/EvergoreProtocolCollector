package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(@JsonProperty("name") String name, @JsonProperty("password") String password, @JsonProperty("next") String next) {}
