package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static java.util.Comparator.naturalOrder;

public class ActivityLine {
	private static final int WINDOW_DAYS = 30;

	private final Map<String, Instant> lastActivityPerAvatar;
	private final Optional<LocalDateTime> cut;

	private ActivityLine(Map<String, Instant> lastActivityPerAvatar, Optional<LocalDateTime> cut) {
		this.lastActivityPerAvatar = lastActivityPerAvatar;
		this.cut = cut;
	}

	public static ActivityLine of(Map<String, Instant> lastBankActivity, Map<String, Instant> lastStorageActivity) {
		Map<String, Instant> latest = new HashMap<>(lastBankActivity);
		lastStorageActivity.forEach((avatar, activity) -> latest.merge(avatar, activity, (bank, storage) -> bank.isAfter(storage) ? bank : storage));
		Optional<Instant> newest = latest.values().stream().max(naturalOrder());

		return new ActivityLine(latest, newest.map(reference -> berlinClock(reference).minusDays(WINDOW_DAYS)));
	}

	public boolean isActive(String avatar) {
		return cut.map(line -> activeSince(avatar, line)).orElse(true);
	}

	private boolean activeSince(String avatar, LocalDateTime line) {
		return Optional.ofNullable(lastActivityPerAvatar.get(avatar)).map(activity -> !berlinClock(activity).isBefore(line)).orElse(false);
	}

	private static LocalDateTime berlinClock(Instant moment) {
		return moment.atZone(APP_ZONE).toLocalDateTime();
	}
}
