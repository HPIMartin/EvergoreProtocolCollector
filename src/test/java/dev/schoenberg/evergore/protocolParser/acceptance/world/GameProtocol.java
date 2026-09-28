package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameProtocol {
	public static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private final List<ProtocolEntry> entries = new CopyOnWriteArrayList<>();

	public void record(LocalDateTime minute, String avatar, String kind, String line) {
		entries.add(new ProtocolEntry(minute, List.of(MINUTE.format(minute) + " " + avatar + " " + kind, line)));
	}

	List<String> page() {
		List<String> page = new ArrayList<>();
		entries.stream().sorted(Comparator.comparing(ProtocolEntry::minute).reversed()).forEach(entry -> page.addAll(entry.lines()));
		return page;
	}

	Optional<LocalDateTime> newestMinute() {
		return entries.stream().map(ProtocolEntry::minute).max(LocalDateTime::compareTo);
	}

	private record ProtocolEntry(LocalDateTime minute, List<String> lines) {}
}
