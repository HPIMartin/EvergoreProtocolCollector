package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

public record UsageWindow(List<AccountUsage> accounts, UsageCounts guild) {}
