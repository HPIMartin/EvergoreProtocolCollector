package dev.schoenberg.evergore.protocolParser.database.bank;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static java.sql.Timestamp.from;

public class BankDatabaseRepository extends LedgerDatabaseRepository<BankEntry, BankDatabaseEntry> implements BankRepository {
	public BankDatabaseRepository(SqliteDatabase database) {
		super(database, BankDatabaseEntry.class);
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
