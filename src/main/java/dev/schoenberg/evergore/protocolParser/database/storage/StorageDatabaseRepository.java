package dev.schoenberg.evergore.protocolParser.database.storage;

import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageSortKey;
import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TYPE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.NAME_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.QUALITY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.QUANTITY_COLUMN;
import static java.sql.Timestamp.from;

public class StorageDatabaseRepository extends LedgerDatabaseRepository<StorageEntry, StorageDatabaseEntry, StorageSortKey> implements StorageRepository {
	public StorageDatabaseRepository(SqliteDatabase database) {
		super(database, StorageDatabaseEntry.class);
	}

	@Override
	protected String columnOf(StorageSortKey key) {
		return switch (key) {
			case TIMESTAMP -> TIMESTAMP_COLUMN;
			case AVATAR -> AVATAR_COLUMN;
			case QUANTITY -> QUANTITY_COLUMN;
			case NAME -> NAME_COLUMN;
			case QUALITY -> QUALITY_COLUMN;
			case TRANSFER_TYPE -> TYPE_COLUMN;
		};
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
