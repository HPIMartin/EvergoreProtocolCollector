package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BankRepositoryStub implements BankRepository {
	private final Map<String, List<BankEntry>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();

	public void seedEntries(String avatar, List<BankEntry> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar) {
		return entriesByAvatar.getOrDefault(avatar, List.of());
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		return avatars;
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
	public Optional<BankEntry> getNewest() {
		throw new UnsupportedOperationException();
	}
}
