package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry;

@DatabaseTable(tableName = StorageDatabaseEntry.TABLE)
public class StorageDatabaseEntry extends LedgerDatabaseEntry {
	public static final String TABLE = "storageEntries";

	@DatabaseField(columnName = "quantity", canBeNull = false, throwIfNull = true)
	public int quantity;

	@DatabaseField(columnName = "name", canBeNull = false)
	public String name;

	@DatabaseField(columnName = "quality", canBeNull = false, throwIfNull = true)
	public int quality;

	public StorageDatabaseEntry(Date timeStamp, String avatar, int quantity, String name, int quality, String type) {
		super(timeStamp, avatar, type);
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
	}

	protected StorageDatabaseEntry() {}
}
