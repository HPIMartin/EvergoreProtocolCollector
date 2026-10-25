package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Singleton;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

@Singleton
class LoginPots {
	private final RateLimitConfiguration configuration;
	private final Clock clock;
	private final Logger logger;
	private final Map<String, LoginPot> potOfAddress = new HashMap<>();
	private final Map<String, LoginPot> potOfAccount = new HashMap<>();
	private final Set<LoginPot> leastRecentlyUsedFirst = new LinkedHashSet<>();
	private final TreeSet<LoginPot> soonestForgottenFirst = new TreeSet<>(Comparator.comparing(LoginPot::forgottenAt).thenComparingLong(LoginPot::sequence));
	private long potsStarted = 0;

	LoginPots(RateLimitConfiguration configuration, Clock clock, Logger logger) {
		this.configuration = configuration;
		this.clock = clock;
		this.logger = logger;
	}

	synchronized boolean turnsAway(String address, String account) {
		Instant now = clock.instant();
		forgetFinishedPots(now);
		LoginPot pot = detached(potLinking(address, account, now));
		pot.addresses().add(address);
		pot.accounts().add(account);
		potOfAddress.put(address, pot);
		potOfAccount.put(account, pot);

		boolean turnedAway = counts(pot, address, now);

		soonestForgottenFirst.add(pot);
		leastRecentlyUsedFirst.remove(pot);
		leastRecentlyUsedFirst.add(pot);
		forgetLeastRecentlyUsedPotsOverBudget();

		return turnedAway;
	}

	synchronized boolean turnsAwayAddress(String address) {
		Instant now = clock.instant();
		forgetFinishedPots(now);
		LoginPot pot = potOfAddress.get(address);

		return pot != null && pot.isTurnedAwayAt(now);
	}

	synchronized List<String> trackedAddresses() {
		return List.copyOf(potOfAddress.keySet());
	}

	synchronized List<String> trackedAccounts() {
		return List.copyOf(potOfAccount.keySet());
	}

	private LoginPot potLinking(String address, String account, Instant now) {
		LoginPot ofAddress = potOfAddress.get(address);
		LoginPot ofAccount = potOfAccount.get(account);
		if (ofAddress == null) {
			return ofAccount == null ? new LoginPot(++potsStarted, configuration.interval(), now) : ofAccount;
		}
		if (ofAccount == null || ofAccount == ofAddress) {
			return ofAddress;
		}

		return joined(ofAddress, ofAccount);
	}

	private LoginPot joined(LoginPot first, LoginPot second) {
		LoginPot larger = first.members() >= second.members() ? first : second;
		LoginPot smaller = larger == first ? second : first;
		detached(larger);
		forget(smaller);
		larger.absorb(smaller);
		smaller.addresses().forEach(address -> potOfAddress.put(address, larger));
		smaller.accounts().forEach(account -> potOfAccount.put(account, larger));

		return larger;
	}

	private LoginPot detached(LoginPot pot) {
		soonestForgottenFirst.remove(pot);
		return pot;
	}

	private boolean counts(LoginPot pot, String address, Instant now) {
		if (pot.isTurnedAwayAt(now)) {
			return true;
		}
		if (pot.countRequest() <= configuration.maxRequestsPerInterval()) {
			return false;
		}

		logger.info("Blocked:" + address);
		pot.turnAwayUntil(now.plus(configuration.blockDuration()));
		return true;
	}

	private void forgetFinishedPots(Instant now) {
		while (!soonestForgottenFirst.isEmpty() && !soonestForgottenFirst.first().forgottenAt().isAfter(now)) {
			forget(soonestForgottenFirst.first());
		}
	}

	private void forgetLeastRecentlyUsedPotsOverBudget() {
		while (potOfAddress.size() > configuration.maxTrackedClients() || potOfAccount.size() > configuration.maxTrackedClients()) {
			forget(leastRecentlyUsedFirst.iterator().next());
		}
	}

	private void forget(LoginPot pot) {
		soonestForgottenFirst.remove(pot);
		leastRecentlyUsedFirst.remove(pot);
		pot.addresses().forEach(potOfAddress::remove);
		pot.accounts().forEach(potOfAccount::remove);
	}
}
