package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewPasswordRequest(@JsonProperty("name") String name, @JsonProperty("issuedPassword") String issuedPassword, @JsonProperty("newPassword") String newPassword) {}
