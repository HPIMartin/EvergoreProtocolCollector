package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Guild {
	private final Set<String> members = new ConcurrentSkipListSet<>();

	public void join(String member) {
		members.add(member);
	}

	public List<String> inGermanOrder() {
		return members.stream().sorted(Collator.getInstance(Locale.GERMAN)).toList();
	}
}
