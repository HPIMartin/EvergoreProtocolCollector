package dev.schoenberg.evergore.protocolParser.database.storage;

import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.sql.Timestamp.*;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class StorageDatabaseRepository extends Repository<StorageDatabaseEntry> implements StorageRepository {
	private final Dao<StorageDatabaseEntry, String> storage;
	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	public static StorageDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		return new StorageDatabaseRepository(con, logger, getDao(con, StorageDatabaseEntry.class));
	}

	private StorageDatabaseRepository(ConnectionSource con, Logger logger, Dao<StorageDatabaseEntry, String> bank) {
		super(con, logger, StorageDatabaseEntry.class);
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
	public void add(List<StorageEntry> newEntries) {
		silentThrow(() -> storage.create(newEntries.stream().map(this::convert).toList()));
	}

	@Override
	public StorageEntry getNewest() {
		return convert(silentThrow(() -> {
			GenericRawResults<String[]> raw = storage.queryRaw("SELECT max(" + TIMESTAMP_COLUMN + ") FROM " + TABLE);

			List<String[]> results = raw.getResults();

			log(results);

			if (results.isEmpty() || results.get(0) == null || results.get(0)[0] == null) {
				return new StorageDatabaseEntry(new Date(Long.MIN_VALUE), "", 0, "", 0, transferTypeVisitor.convert(TransferType.EINLAGERUNG));
			}

			Timestamp highestTimeStamp = valueOf(results.get(0)[0]);
			return storage.queryBuilder().where().eq(StorageDatabaseEntry.TIMESTAMP_COLUMN, highestTimeStamp).queryForFirst();
		}));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<StorageDatabaseEntry> avatars = silentThrow(() -> storage.queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(bde -> bde.avatar).toList();
	}

	private void log(List<String[]> results) {
		boolean first_x = true;
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String[] x : results) {
			if (!first_x) {
				sb.append(",");
			}
			first_x = false;
			boolean first_y = true;
			sb.append("[");
			for (String y : x) {
				if (!first_y) {
					sb.append(",");
				}
				sb.append(y);
				first_y = false;
			}
			sb.append("]");
		}
		sb.append("]");

		logger.debug(sb.toString());
	}

	private List<StorageEntry> convert(List<StorageDatabaseEntry> dbEntries) {
		return dbEntries.stream().map(this::convert).toList();
	}

	private StorageEntry convert(StorageDatabaseEntry dbEntry) {
		return new StorageEntry(dbEntry.timeStamp.toInstant(), dbEntry.avatar, dbEntry.quantity, dbEntry.name, dbEntry.quality,
				transferTypeVisitor.convert(dbEntry.type));
	}

	private StorageDatabaseEntry convert(StorageEntry entry) {
		return new StorageDatabaseEntry(from(entry.timeStamp), entry.avatar, entry.quantity, entry.name, entry.quality,
				transferTypeVisitor.convert(entry.type));
	}
}
