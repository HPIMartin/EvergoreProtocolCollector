package dev.schoenberg.evergore.protocolParser.database.bank;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry;

@DatabaseTable(tableName = BankDatabaseEntry.TABLE)
public class BankDatabaseEntry extends LedgerDatabaseEntry {
	public static final String TABLE = "bankEntries";

	@DatabaseField(columnName = "amount", canBeNull = false, throwIfNull = true)
	public int amount;

	public BankDatabaseEntry(Date timeStamp, String avatar, int amount, String type) {
		super(timeStamp, avatar, type);
		this.amount = amount;
	}

	protected BankDatabaseEntry() {}
}
