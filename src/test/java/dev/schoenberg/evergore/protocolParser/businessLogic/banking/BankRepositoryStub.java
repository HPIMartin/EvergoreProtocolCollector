package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toMap;

public class BankRepositoryStub implements BankRepository {
	private final Map<String, List<BankEntry>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();
	private final Set<String> unreadableAvatars = new HashSet<>();

	public void seedEntries(String avatar, List<BankEntry> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar) {
		if (unreadableAvatars.contains(avatar)) {
			throw new IllegalStateException("the ledger of " + avatar + " cannot be read");
		}
		return entriesByAvatar.getOrDefault(avatar, List.of());
	}

	public void failOn(String avatar) {
		unreadableAvatars.add(avatar);
	}

	@Override
	public long countFor(String avatar) {
		return getAllFor(avatar).size();
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		return avatars;
	}

	@Override
	public Map<String, Instant> latestTimestampPerAvatar() {
		return entriesByAvatar
				.entrySet()
				.stream()
				.filter(byAvatar -> !byAvatar.getValue().isEmpty())
				.collect(toMap(Map.Entry::getKey, byAvatar -> byAvatar.getValue().stream().map(BankEntry::timeStamp).max(naturalOrder()).orElseThrow()));
	}

	@Override
	public void add(List<BankEntry> newEntries) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, long page, long size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BankEntry> getAllSince(Instant timestampInclusive) {
		throw new UnsupportedOperationException();
	}
}
