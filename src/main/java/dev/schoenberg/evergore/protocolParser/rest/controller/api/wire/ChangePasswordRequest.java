package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangePasswordRequest(@JsonProperty("currentPassword") String currentPassword, @JsonProperty("newPassword") String newPassword) {}
