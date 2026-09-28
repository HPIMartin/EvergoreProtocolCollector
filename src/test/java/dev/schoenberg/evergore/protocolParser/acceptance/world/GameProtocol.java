package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GameProtocol {
	public static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	public static final LocalDateTime BASE_MOVEMENT = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final Pattern HEADLINE_MINUTE = Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}) ");

	private final List<ProtocolEntry> entries = new CopyOnWriteArrayList<>();
	private volatile List<String> text = List.of();

	public void record(LocalDateTime minute, String avatar, String kind, String line) {
		if (!text.isEmpty()) {
			throw new IllegalStateException("The scenario records a movement into a protocol it already showed as text");
		}
		entries.add(new ProtocolEntry(minute, List.of(MINUTE.format(minute) + " " + avatar + " " + kind, line)));
	}

	public void show(List<String> lines) {
		entries.clear();
		text = List.copyOf(lines);
	}

	List<String> page() {
		List<String> page = new ArrayList<>(text);
		entries.stream().sorted(Comparator.comparing(ProtocolEntry::minute).reversed()).forEach(entry -> page.addAll(entry.lines()));
		return page;
	}

	Optional<LocalDateTime> newestMinute() {
		Stream<LocalDateTime> recorded = entries.stream().map(ProtocolEntry::minute);
		Stream<LocalDateTime> shown = text.stream().map(GameProtocol::minuteOfHeadline).flatMap(Optional::stream);
		return Stream.concat(recorded, shown).max(LocalDateTime::compareTo);
	}

	private static Optional<LocalDateTime> minuteOfHeadline(String line) {
		Matcher headline = HEADLINE_MINUTE.matcher(line);
		if (!headline.find()) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDateTime.parse(headline.group(1), MINUTE));
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	private record ProtocolEntry(LocalDateTime minute, List<String> lines) {}
}
