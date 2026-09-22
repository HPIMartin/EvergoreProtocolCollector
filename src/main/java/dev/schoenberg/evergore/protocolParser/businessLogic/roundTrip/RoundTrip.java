package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

public record RoundTrip(String avatar, EvergoreItem item, int quantity) {}
