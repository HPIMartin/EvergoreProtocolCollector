package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import static java.lang.Long.*;
import static java.lang.String.valueOf;
import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.*;

import java.time.*;
import java.time.format.*;

public abstract class MetaInformationKey<T> {
	private static final DateTimeKey LAST_UPDATED = new DateTimeKey("last_updated");

	public final String id;

	protected MetaInformationKey(String id) {
		this.id = id;
	}

	public abstract String serialize(T value);

	public abstract T deserialize(String raw);

	public static MetaInformationKey<LocalDateTime> getLastUpdatedKey() {
		return LAST_UPDATED;
	}

	public static MetaInformationKey<Long> getBankPlacement(String avatar) {
		return new LongKey("bank_placement_" + avatar);
	}

	public static MetaInformationKey<Long> getBankWithdrawl(String avatar) {
		return new LongKey("bank_withdrawl_" + avatar);
	}

	private static class DateTimeKey extends MetaInformationKey<LocalDateTime> {
		private static final DateTimeFormatter DATE_TIME_PATTERN = ofPattern("dd.MM.yyyy HH:mm");

		private DateTimeKey(String id) {
			super(id);
		}

		@Override
		public String serialize(LocalDateTime value) {
			return value.format(DATE_TIME_PATTERN);
		}

		@Override
		public LocalDateTime deserialize(String raw) {
			return parse(raw, DATE_TIME_PATTERN);
		}
	}

	private static class LongKey extends MetaInformationKey<Long> {
		private LongKey(String id) {
			super(id);
		}

		@Override
		public String serialize(Long value) {
			return valueOf(value);
		}

		@Override
		public Long deserialize(String raw) {
			return parseLong(raw);
		}
	}
}
