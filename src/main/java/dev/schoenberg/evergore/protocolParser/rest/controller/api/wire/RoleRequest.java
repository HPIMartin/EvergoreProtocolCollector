package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoleRequest(@JsonProperty("role") String role) {}
