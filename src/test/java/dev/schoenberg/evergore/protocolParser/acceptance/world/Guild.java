package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;

public class Guild {
	private final Set<String> members = new ConcurrentSkipListSet<>();

	public void join(String member) {
		members.add(member);
	}

	public List<String> inGermanOrder() {
		return members.stream().sorted(GermanOrder.NAMES).toList();
	}
}
