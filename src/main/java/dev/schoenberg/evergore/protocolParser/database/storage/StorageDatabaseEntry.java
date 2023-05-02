package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.*;

import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

@DatabaseTable(tableName = StorageDatabaseEntry.TABLE)
public class StorageDatabaseEntry {
	public static final String TABLE = "storageEntries";
	public static final String AVATAR_COLUMN = "avatar";
	public static final String TIMESTAMP_COLUMN = "timeStamp";

	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	public UUID id;

	@DatabaseField(columnName = TIMESTAMP_COLUMN, dataType = DataType.DATE_STRING)
	public Date timeStamp;

	@DatabaseField(columnName = AVATAR_COLUMN)
	public String avatar;

	@DatabaseField(columnName = "quantity")
	public int quantity;

	@DatabaseField(columnName = "name")
	public String name;

	@DatabaseField(columnName = "quality")
	public int quality;

	@DatabaseField(columnName = "type")
	public String type;

	public StorageDatabaseEntry(Date timeStamp, String avatar, int quantity, String name, int quality, String type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
		this.type = type;
	}

	protected StorageDatabaseEntry() {
		// ORMLite needs a no-arg constructor
	}
}
