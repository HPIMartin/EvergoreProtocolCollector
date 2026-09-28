package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;

public class GameProtocols {
	private final GameProtocol bank = new GameProtocol();
	private final GameProtocol storage = new GameProtocol();

	public GameProtocol bank() {
		return bank;
	}

	public GameProtocol storage() {
		return storage;
	}

	public PageContents pageContents() {
		return new PageContents(storage.page(), bank.page());
	}

	public Optional<LocalDateTime> newestMinute() {
		return Stream.of(bank.newestMinute(), storage.newestMinute()).flatMap(Optional::stream).max(LocalDateTime::compareTo);
	}
}
