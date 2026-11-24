package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record AdminPage(List<String> messages, List<ShownFindingList> findings, List<ShownAccount> accounts, String question, String startButton) {
	public ShownAccount accountNamed(String name) {
		return accounts.stream().filter(account -> name.equals(account.name())).findFirst().orElseThrow(() -> new AssertionError("The admin page lists no account of " + name));
	}
}
