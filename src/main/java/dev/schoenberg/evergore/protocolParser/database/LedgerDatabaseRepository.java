package dev.schoenberg.evergore.protocolParser.database;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;

import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.sql.Timestamp.from;
import static java.util.stream.Collectors.toMap;

public abstract class LedgerDatabaseRepository<E, R extends LedgerDatabaseEntry> implements LedgerRepository<E> {
	private final SqliteDatabase database;
	private final Class<R> rowType;
	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	protected LedgerDatabaseRepository(SqliteDatabase database, Class<R> rowType) {
		this.database = database;
		this.rowType = rowType;
	}

	@Override
	public List<E> getAllFor(String avatar, long page, long size) {
		List<R> result = silentThrow(() -> rows().queryBuilder().orderBy(TIMESTAMP_COLUMN, false).limit(size).offset(page * size).where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return toEntries(result);
	}

	@Override
	public List<E> getAllFor(String avatar) {
		List<R> result = silentThrow(() -> rows().queryBuilder().where().eq(AVATAR_COLUMN, avatar).query());

		return toEntries(result);
	}

	@Override
	public long countFor(String avatar) {
		return silentThrow(() -> rows().queryBuilder().where().eq(AVATAR_COLUMN, avatar).countOf());
	}

	@Override
	public List<E> getAllSince(Instant timestampInclusive) {
		List<R> result = silentThrow(() -> rows().queryBuilder().where().ge(TIMESTAMP_COLUMN, from(timestampInclusive)).query());

		return toEntries(result);
	}

	@Override
	public void add(List<E> newEntries) {
		Dao<R, String> rows = rows();
		List<R> newRows = newEntries.stream().map(this::toRow).toList();
		silentThrow(() -> rows.callBatchTasks(() -> {
			for (R row : newRows) {
				rows.create(row);
			}
			return null;
		}));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<R> avatars = silentThrow(() -> rows().queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(row -> row.avatar).toList();
	}

	@Override
	public Map<String, Instant> latestTimestampPerAvatar() {
		Dao<R, String> rows = rows();
		String newestPerAvatar = "SELECT " + AVATAR_COLUMN + ", MAX(" + TIMESTAMP_COLUMN + ") AS " + TIMESTAMP_COLUMN + " FROM " + rows.getTableName() + " GROUP BY "
				+ AVATAR_COLUMN;

		return silentThrow(() -> {
			GenericRawResults<R> newest = rows.queryRaw(newestPerAvatar, rows.getRawRowMapper());
			try {
				return newest.getResults().stream().collect(toMap(row -> row.avatar, row -> row.timeStamp.toInstant()));
			} finally {
				newest.close();
			}
		});
	}

	protected abstract E toEntry(R row);

	protected abstract R toRow(E entry);

	protected TransferType transferTypeOf(R row) {
		return transferTypeVisitor.convert(row.type);
	}

	protected String storedFormOf(TransferType type) {
		return transferTypeVisitor.convert(type);
	}

	private Dao<R, String> rows() {
		return database.dao(rowType);
	}

	private List<E> toEntries(List<R> rows) {
		return rows.stream().map(this::toEntry).toList();
	}
}
