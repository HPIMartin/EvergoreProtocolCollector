package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.*;
import java.util.*;

public class BankRepositoryStub implements BankRepository {
	private final Map<String, List<BankEntry>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();
	private LocalDateTime capturedAfter;

	public void seedEntries(String avatar, List<BankEntry> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	public LocalDateTime capturedAfter() {
		return capturedAfter;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, LocalDateTime after) {
		capturedAfter = after;
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
	public BankEntry getNewest() {
		throw new UnsupportedOperationException();
	}
}
