package dev.schoenberg.evergore.protocolParser.database.usage;

import java.util.List;

import com.j256.ormlite.dao.Dao;

import dev.schoenberg.evergore.protocolParser.application.UsageRecordRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class UsageRecordDatabaseRepository implements UsageRecordRepository {
	private final SqliteDatabase database;

	public UsageRecordDatabaseRepository(SqliteDatabase database) {
		this.database = database;
	}

	@Override
	public void addAll(List<UsageRecord> batch) {
		Dao<UsageRecordEntry, String> records = database.dao(UsageRecordEntry.class);
		batch.forEach(record -> silentThrow(() -> records.create(entryOf(record))));
	}

	private static UsageRecordEntry entryOf(UsageRecord record) {
		return new UsageRecordEntry(record.accountKey(), record.at().toEpochMilli(), record.kind(), record.detail().orElse(null));
	}
}
