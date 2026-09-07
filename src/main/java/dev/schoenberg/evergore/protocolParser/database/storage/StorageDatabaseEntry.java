package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.Date;
import java.util.UUID;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = StorageDatabaseEntry.TABLE)
public class StorageDatabaseEntry {
	public static final String TABLE = "storageEntries";
	public static final String AVATAR_COLUMN = "avatar";
	public static final String TIMESTAMP_COLUMN = "timeStamp";
	public static final String ID_COLUMN = "id";

	@DatabaseField(columnName = ID_COLUMN, generatedId = true, allowGeneratedIdInsert = true, canBeNull = false)
	public UUID id;

	@DatabaseField(columnName = TIMESTAMP_COLUMN, dataType = DataType.DATE_STRING, canBeNull = false)
	public Date timeStamp;

	@DatabaseField(columnName = AVATAR_COLUMN, canBeNull = false)
	public String avatar;

	@DatabaseField(columnName = "quantity", canBeNull = false, throwIfNull = true)
	public int quantity;

	@DatabaseField(columnName = "name", canBeNull = false)
	public String name;

	@DatabaseField(columnName = "quality", canBeNull = false, throwIfNull = true)
	public int quality;

	@DatabaseField(columnName = "type", canBeNull = false)
	public String type;

	public StorageDatabaseEntry(Date timeStamp, String avatar, int quantity, String name, int quality, String type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
		this.type = type;
	}

	protected StorageDatabaseEntry() {}
}
