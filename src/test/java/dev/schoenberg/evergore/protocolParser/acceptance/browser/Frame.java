package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record Frame(String loggedInAs, List<String> buttons) {}
