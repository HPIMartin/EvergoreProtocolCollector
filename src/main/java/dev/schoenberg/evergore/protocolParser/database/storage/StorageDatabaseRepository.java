package dev.schoenberg.evergore.protocolParser.database.storage;

import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageSortKey;
import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.SortColumn;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TYPE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.SortColumn.germanOrderOf;
import static dev.schoenberg.evergore.protocolParser.database.SortColumn.of;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.NAME_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.QUALITY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.QUANTITY_COLUMN;
import static java.sql.Timestamp.from;

public class StorageDatabaseRepository extends LedgerDatabaseRepository<StorageEntry, StorageDatabaseEntry, StorageSortKey> implements StorageRepository {
	public StorageDatabaseRepository(SqliteDatabase database) {
		super(database, StorageDatabaseEntry.class);
	}

	@Override
	protected SortColumn columnOf(StorageSortKey key) {
		return switch (key) {
			case TIMESTAMP -> of(TIMESTAMP_COLUMN);
			case AVATAR -> of(AVATAR_COLUMN);
			case QUANTITY -> of(QUANTITY_COLUMN);
			case NAME -> germanOrderOf(NAME_COLUMN);
			case QUALITY -> of(QUALITY_COLUMN);
			case TRANSFER_TYPE -> of(TYPE_COLUMN);
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
