package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record Ledger(String caption, List<String> headers, List<List<String>> rows, String emptyMessage) {}
