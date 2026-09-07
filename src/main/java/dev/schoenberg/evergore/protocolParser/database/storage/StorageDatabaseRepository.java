package dev.schoenberg.evergore.protocolParser.database.storage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.Repository;
import dev.schoenberg.evergore.protocolParser.database.TransferTypeDatabaseVisitor;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.sql.Timestamp.from;
import static java.util.stream.Collectors.toMap;

public class StorageDatabaseRepository extends Repository<StorageDatabaseEntry> implements StorageRepository {
	private final Dao<StorageDatabaseEntry, String> storage;
	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	public static StorageDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		StorageDatabaseRepository repository = new StorageDatabaseRepository(con, logger, getDao(con, StorageDatabaseEntry.class));
		return repository;
	}

	private StorageDatabaseRepository(ConnectionSource con, Logger logger, Dao<StorageDatabaseEntry, String> bank) {
		super(con, logger);
		storage = bank;
	}

	@Override
	public List<StorageEntry> getAllFor(String avatar, long page, long size) {
		List<StorageDatabaseEntry> result = silentThrow(
				() -> storage.queryBuilder().limit(size).offset(page * size).orderBy(TIMESTAMP_COLUMN, false).where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return convert(result);
	}

	@Override
	public List<StorageEntry> getAllFor(String avatar) {
		List<StorageDatabaseEntry> result = silentThrow(() -> storage.queryBuilder().where().eq(AVATAR_COLUMN, avatar).query());

		return convert(result);
	}

	@Override
	public long countFor(String avatar) {
		return silentThrow(() -> storage.queryBuilder().where().eq(AVATAR_COLUMN, avatar).countOf());
	}

	@Override
	public List<StorageEntry> getAllSince(Instant timestampInclusive) {
		List<StorageDatabaseEntry> result = silentThrow(() -> storage.queryBuilder().where().ge(StorageDatabaseEntry.TIMESTAMP_COLUMN, from(timestampInclusive)).query());

		return convert(result);
	}

	@Override
	public void add(List<StorageEntry> newEntries) {
		silentThrow(() -> storage.create(newEntries.stream().map(this::convert).toList()));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<StorageDatabaseEntry> avatars = silentThrow(() -> storage.queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(bde -> bde.avatar).toList();
	}

	@Override
	public Map<String, Instant> latestTimestampPerAvatar() {
		String newestPerAvatar = "SELECT " + AVATAR_COLUMN + ", MAX(" + TIMESTAMP_COLUMN + ") AS " + TIMESTAMP_COLUMN + " FROM " + StorageDatabaseEntry.TABLE + " GROUP BY "
				+ AVATAR_COLUMN;

		return silentThrow(() -> {
			GenericRawResults<StorageDatabaseEntry> rows = storage.queryRaw(newestPerAvatar, storage.getRawRowMapper());
			try {
				return rows.getResults().stream().collect(toMap(row -> row.avatar, row -> row.timeStamp.toInstant()));
			} finally {
				rows.close();
			}
		});
	}

	private List<StorageEntry> convert(List<StorageDatabaseEntry> dbEntries) {
		return dbEntries.stream().map(this::convert).toList();
	}

	private StorageEntry convert(StorageDatabaseEntry dbEntry) {
		return new StorageEntry(dbEntry.timeStamp.toInstant(), dbEntry.avatar, dbEntry.quantity, dbEntry.name, dbEntry.quality, transferTypeVisitor.convert(dbEntry.type));
	}

	private StorageDatabaseEntry convert(StorageEntry entry) {
		return new StorageDatabaseEntry(from(entry.timeStamp()), entry.avatar(), entry.quantity(), entry.name(), entry.quality(), transferTypeVisitor.convert(entry.type()));
	}
}
