package dev.schoenberg.evergore.protocolParser.businessLogic.base;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toMap;

public abstract class LedgerRepositoryStub<E, K> implements LedgerRepository<E, K> {
	private final Map<String, List<E>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();
	private final Set<String> unreadableAvatars = new HashSet<>();
	private final Function<E, Instant> timeStampOf;

	protected LedgerRepositoryStub(Function<E, Instant> timeStampOf) {
		this.timeStampOf = timeStampOf;
	}

	public void seedEntries(String avatar, List<E> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	@Override
	public List<E> getAllFor(String avatar) {
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
				.collect(toMap(Map.Entry::getKey, byAvatar -> byAvatar.getValue().stream().map(timeStampOf).max(naturalOrder()).orElseThrow()));
	}

	@Override
	public void add(List<E> newEntries) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> getAllFor(String avatar, LedgerSort<K> sort, long page, long size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> getAllSince(Instant timestampInclusive) {
		throw new UnsupportedOperationException();
	}
}
