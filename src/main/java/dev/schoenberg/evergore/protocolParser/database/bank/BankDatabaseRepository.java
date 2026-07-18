package dev.schoenberg.evergore.protocolParser.database.bank;

import java.time.Instant;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.Repository;
import dev.schoenberg.evergore.protocolParser.database.TransferTypeDatabaseVisitor;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.sql.Timestamp.from;

public class BankDatabaseRepository extends Repository<BankDatabaseEntry> implements BankRepository {
	private final Dao<BankDatabaseEntry, String> bank;

	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	public static BankDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		BankDatabaseRepository repository = new BankDatabaseRepository(con, logger, getDao(con, BankDatabaseEntry.class));
		repository.ensureTable();
		return repository;
	}

	private BankDatabaseRepository(ConnectionSource con, Logger logger, Dao<BankDatabaseEntry, String> bank) {
		super(con, logger, BankDatabaseEntry.class);
		this.bank = bank;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, long page, long size) {
		List<BankDatabaseEntry> result = silentThrow(
				() -> bank.queryBuilder().orderBy(TIMESTAMP_COLUMN, false).limit(size).offset(page * size).where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return convert(result);
	}

	@Override
	public List<BankEntry> getAllFor(String avatar) {
		List<BankDatabaseEntry> result = silentThrow(() -> bank.queryBuilder().where().eq(AVATAR_COLUMN, avatar).query());

		return convert(result);
	}

	@Override
	public List<BankEntry> getAllSince(Instant timestampInclusive) {
		List<BankDatabaseEntry> result = silentThrow(() -> bank.queryBuilder().where().ge(BankDatabaseEntry.TIMESTAMP_COLUMN, from(timestampInclusive)).query());

		return convert(result);
	}

	@Override
	public void add(List<BankEntry> newEntries) {
		silentThrow(() -> bank.create(newEntries.stream().map(this::convert).toList()));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<BankDatabaseEntry> avatars = silentThrow(() -> bank.queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(bde -> bde.avatar).toList();
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
