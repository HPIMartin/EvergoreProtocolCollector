package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.Optional;

public record AvatarContribution(String avatar, Optional<Contribution> contribution, Instant lastBankActivity, Instant lastStorageActivity, Instant staleSumsFrom) {}
