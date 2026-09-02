package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;

public record AvatarContribution(String avatar, Contribution contribution, Instant lastBankActivity, Instant lastStorageActivity) {}
