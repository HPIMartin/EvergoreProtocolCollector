package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record ShownFindingList(String heading, String text, List<ShownFinding> entries, List<ShownLink> links) {}
