package dev.schoenberg.evergore.protocolParser.database.usage;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;

import dev.schoenberg.evergore.protocolParser.application.UsageRecordReader;
import dev.schoenberg.evergore.protocolParser.application.UsageRecordRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class UsageRecordDatabaseRepository implements UsageRecordRepository, UsageRecordReader {
	private static final String ACCOUNT_COLUMN = "account_key";
	private static final String AT_COLUMN = "at_epoch_millis";
	private static final String WHAT_COLUMN = "what";

	private final SqliteDatabase database;

	public UsageRecordDatabaseRepository(SqliteDatabase database) {
		this.database = database;
	}

	@Override
	public void addAll(List<UsageRecord> batch) {
		Dao<UsageRecordEntry, String> records = database.dao(UsageRecordEntry.class);
		database.inTransaction(() -> {
			for (UsageRecord record : batch) {
				records.create(entryOf(record));
			}
			return batch.size();
		});
	}

	@Override
	public Map<String, Map<UsageKind, Integer>> countsSince(Instant from) {
		Dao<UsageRecordEntry, String> records = database.dao(UsageRecordEntry.class);
		List<String[]> rows = silentThrow(() -> records
				.queryBuilder()
				.selectRaw(ACCOUNT_COLUMN, WHAT_COLUMN, "count(*)")
				.groupBy(ACCOUNT_COLUMN)
				.groupBy(WHAT_COLUMN)
				.where()
				.ge(AT_COLUMN, from.toEpochMilli())
				.queryRaw()
				.getResults());
		Map<String, Map<UsageKind, Integer>> counts = new HashMap<>();
		for (String[] row : rows) {
			counts.computeIfAbsent(row[0], account -> new EnumMap<>(UsageKind.class)).put(UsageKind.valueOf(row[1]), Integer.parseInt(row[2]));
		}
		return counts;
	}

	private static UsageRecordEntry entryOf(UsageRecord record) {
		return new UsageRecordEntry(record.accountKey(), record.at().toEpochMilli(), record.kind(), record.detail().orElse(null));
	}
}
