package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public record AccountListing(String name, Role role, boolean resting, Optional<Instant> latestLogin) {}
