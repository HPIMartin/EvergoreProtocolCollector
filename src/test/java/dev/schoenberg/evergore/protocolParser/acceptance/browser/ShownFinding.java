package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record ShownFinding(String text, List<ShownLink> links) {}
