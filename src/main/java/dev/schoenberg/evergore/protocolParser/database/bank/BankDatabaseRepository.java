package dev.schoenberg.evergore.protocolParser.database.bank;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.SortColumn;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry.TYPE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.SortColumn.of;
import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.AMOUNT_COLUMN;
import static java.sql.Timestamp.from;

public class BankDatabaseRepository extends LedgerDatabaseRepository<BankEntry, BankDatabaseEntry, BankSortKey> implements BankRepository {
	public BankDatabaseRepository(SqliteDatabase database) {
		super(database, BankDatabaseEntry.class);
	}

	@Override
	protected SortColumn columnOf(BankSortKey key) {
		return switch (key) {
			case TIMESTAMP -> of(TIMESTAMP_COLUMN);
			case AVATAR -> of(AVATAR_COLUMN);
			case AMOUNT -> of(AMOUNT_COLUMN);
			case TRANSFER_TYPE -> of(TYPE_COLUMN);
		};
	}

	@Override
	protected BankEntry toEntry(BankDatabaseEntry row) {
		return new BankEntry(row.timeStamp.toInstant(), row.avatar, row.amount, transferTypeOf(row));
	}

	@Override
	protected BankDatabaseEntry toRow(BankEntry entry) {
		return new BankDatabaseEntry(from(entry.timeStamp()), entry.avatar(), entry.amount(), storedFormOf(entry.type()));
	}
}
