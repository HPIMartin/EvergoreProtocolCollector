package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import dev.schoenberg.evergore.protocolParser.database.LedgerDatabaseEntry;

@DatabaseTable(tableName = StorageDatabaseEntry.TABLE)
public class StorageDatabaseEntry extends LedgerDatabaseEntry {
	public static final String TABLE = "storageEntries";
	public static final String QUANTITY_COLUMN = "quantity";
	public static final String NAME_COLUMN = "name";
	public static final String QUALITY_COLUMN = "quality";

	@DatabaseField(columnName = QUANTITY_COLUMN, canBeNull = false, throwIfNull = true)
	public int quantity;

	@DatabaseField(columnName = NAME_COLUMN, canBeNull = false)
	public String name;

	@DatabaseField(columnName = QUALITY_COLUMN, canBeNull = false, throwIfNull = true)
	public int quality;

	public StorageDatabaseEntry(Date timeStamp, String avatar, int quantity, String name, int quality, String type) {
		super(timeStamp, avatar, type);
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
	}

	protected StorageDatabaseEntry() {}
}
