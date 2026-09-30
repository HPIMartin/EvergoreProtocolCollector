package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.Optional;

public record WholeGoldContribution(long bankDeposited, long bankWithdrawn, long storageDeposited, long storageWithdrawn, long net, Optional<WholeGoldShare> guildShare) {}
