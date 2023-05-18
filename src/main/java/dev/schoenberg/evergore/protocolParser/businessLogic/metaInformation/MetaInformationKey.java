package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.*;

import java.time.*;
import java.time.format.*;

public abstract class MetaInformationKey<T> {
	public final String id;

	protected MetaInformationKey(String id) {
		this.id = id;
	}

	public abstract String serialize(T value);

	public abstract T deserialize(String raw);

	public static final MetaInformationKey<LocalDateTime> LAST_UPDATED = new MetaInformationKey<>("last_updated") {
		private final DateTimeFormatter DATE_TIME_PATTERN = ofPattern("dd.MM.yyyy HH:mm");

		@Override
		public String serialize(LocalDateTime value) {
			return value.format(DATE_TIME_PATTERN);
		}

		@Override
		public LocalDateTime deserialize(String raw) {
			return parse(raw, DATE_TIME_PATTERN);
		}
	};
}
