package dev.schoenberg.evergore.protocolParser.database.bank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.TransferTypeDatabaseVisitor;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;

import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.sql.Timestamp.from;
import static java.util.stream.Collectors.toMap;

public class BankDatabaseRepository implements BankRepository {
	private final SqliteDatabase database;

	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	public BankDatabaseRepository(SqliteDatabase database) {
		this.database = database;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, long page, long size) {
		List<BankDatabaseEntry> result = silentThrow(
				() -> bank().queryBuilder().orderBy(TIMESTAMP_COLUMN, false).limit(size).offset(page * size).where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return convert(result);
	}

	@Override
	public List<BankEntry> getAllFor(String avatar) {
		List<BankDatabaseEntry> result = silentThrow(() -> bank().queryBuilder().where().eq(AVATAR_COLUMN, avatar).query());

		return convert(result);
	}

	@Override
	public long countFor(String avatar) {
		return silentThrow(() -> bank().queryBuilder().where().eq(AVATAR_COLUMN, avatar).countOf());
	}

	@Override
	public List<BankEntry> getAllSince(Instant timestampInclusive) {
		List<BankDatabaseEntry> result = silentThrow(() -> bank().queryBuilder().where().ge(TIMESTAMP_COLUMN, from(timestampInclusive)).query());

		return convert(result);
	}

	@Override
	public void add(List<BankEntry> newEntries) {
		Dao<BankDatabaseEntry, String> bank = bank();
		List<BankDatabaseEntry> rows = newEntries.stream().map(this::convert).toList();
		silentThrow(() -> bank.callBatchTasks(() -> {
			for (BankDatabaseEntry row : rows) {
				bank.create(row);
			}
			return null;
		}));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<BankDatabaseEntry> avatars = silentThrow(() -> bank().queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(bde -> bde.avatar).toList();
	}

	@Override
	public Map<String, Instant> latestTimestampPerAvatar() {
		String newestPerAvatar = "SELECT " + AVATAR_COLUMN + ", MAX(" + TIMESTAMP_COLUMN + ") AS " + TIMESTAMP_COLUMN + " FROM " + BankDatabaseEntry.TABLE + " GROUP BY "
				+ AVATAR_COLUMN;

		return silentThrow(() -> {
			Dao<BankDatabaseEntry, String> bank = bank();
			GenericRawResults<BankDatabaseEntry> rows = bank.queryRaw(newestPerAvatar, bank.getRawRowMapper());
			try {
				return rows.getResults().stream().collect(toMap(row -> row.avatar, row -> row.timeStamp.toInstant()));
			} finally {
				rows.close();
			}
		});
	}

	private Dao<BankDatabaseEntry, String> bank() {
		return database.dao(BankDatabaseEntry.class);
	}

	private List<BankEntry> convert(List<BankDatabaseEntry> dbEntries) {
		return dbEntries.stream().map(this::convert).toList();
	}

	private BankEntry convert(BankDatabaseEntry dbEntry) {
		return new BankEntry(dbEntry.timeStamp.toInstant(), dbEntry.avatar, dbEntry.amount, transferTypeVisitor.convert(dbEntry.type));
	}

	private BankDatabaseEntry convert(BankEntry entry) {
		return new BankDatabaseEntry(from(entry.timeStamp()), entry.avatar(), entry.amount(), transferTypeVisitor.convert(entry.type()));
	}
}
