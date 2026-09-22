package dev.schoenberg.evergore.protocolParser.database.storage;

import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static java.sql.Timestamp.from;

public class StorageDatabaseRepository extends LedgerDatabaseRepository<StorageEntry, StorageDatabaseEntry> implements StorageRepository {
	public StorageDatabaseRepository(SqliteDatabase database) {
		super(database, StorageDatabaseEntry.class);
	}

	@Override
	protected StorageEntry toEntry(StorageDatabaseEntry row) {
		return new StorageEntry(row.timeStamp.toInstant(), row.avatar, row.quantity, row.name, row.quality, transferTypeOf(row));
	}

	@Override
	protected StorageDatabaseEntry toRow(StorageEntry entry) {
		return new StorageDatabaseEntry(from(entry.timeStamp()), entry.avatar(), entry.quantity(), entry.name(), entry.quality(), storedFormOf(entry.type()));
	}
}
