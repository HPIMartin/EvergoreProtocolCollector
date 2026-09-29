package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;

public class GameProtocols {
	private final GameProtocol bank = new GameProtocol();
	private final GameProtocol storage = new GameProtocol();
	private volatile boolean reachable = true;

	public GameProtocol bank() {
		return bank;
	}

	public GameProtocol storage() {
		return storage;
	}

	public void showNoEntry() {
		bank.show(List.of());
		storage.show(List.of());
	}

	public void cutOff() {
		reachable = false;
	}

	public void reconnect() {
		reachable = true;
	}

	public boolean reachable() {
		return reachable;
	}

	public PageContents pageContents() {
		if (!reachable) {
			throw new IllegalStateException("The acceptance scenario made the game unreachable");
		}
		return new PageContents(storage.page(), bank.page());
	}

	public Optional<LocalDateTime> newestMinute() {
		return Stream.of(bank.newestMinute(), storage.newestMinute()).flatMap(Optional::stream).max(LocalDateTime::compareTo);
	}
}
