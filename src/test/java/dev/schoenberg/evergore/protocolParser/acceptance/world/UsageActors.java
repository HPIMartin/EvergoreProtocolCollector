package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

public class UsageActors {
	private final Map<String, Instant> loggedInAt = new HashMap<>();
	private final Map<String, MemberBrowser> browsers = new HashMap<>();
	private String lastMember;
	private Instant adminLoggedInAt;
	private MemberBrowser adminsBrowser;

	public void memberLoggedIn(String name, Instant at, MemberBrowser on) {
		loggedInAt.put(name, at);
		browsers.put(name, on);
		lastMember = name;
		if (on == adminsBrowser) {
			adminsBrowser = null;
		}
	}

	public void adminLoggedIn(Instant at, MemberBrowser on) {
		adminLoggedInAt = at;
		adminsBrowser = on;
	}

	public Instant loggedInAt(String name) {
		Instant at = loggedInAt.get(name);
		if (at == null) {
			throw new AssertionError(name + " has not logged in in this scenario");
		}
		return at;
	}

	public MemberBrowser browserOf(String name) {
		MemberBrowser browser = browsers.get(name);
		if (browser == null) {
			throw new AssertionError(name + " has no browser in this scenario");
		}
		return browser;
	}

	public String lastMember() {
		if (lastMember == null) {
			throw new AssertionError("no member has logged in in this scenario");
		}
		return lastMember;
	}

	public Instant adminLoggedInAt() {
		if (adminLoggedInAt == null) {
			throw new AssertionError("the admin has not logged in in this scenario");
		}
		return adminLoggedInAt;
	}

	public Optional<MemberBrowser> adminsBrowserWithALogin() {
		return Optional.ofNullable(adminsBrowser);
	}
}
