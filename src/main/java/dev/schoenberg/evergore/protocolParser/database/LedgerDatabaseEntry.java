package dev.schoenberg.evergore.protocolParser.database;

import java.util.Date;
import java.util.UUID;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public abstract class LedgerDatabaseEntry {
	public static final String ID_COLUMN = "id";
	public static final String TIMESTAMP_COLUMN = "timeStamp";
	public static final String AVATAR_COLUMN = "avatar";

	@DatabaseField(columnName = ID_COLUMN, generatedId = true, allowGeneratedIdInsert = true, canBeNull = false)
	public UUID id;

	@DatabaseField(columnName = TIMESTAMP_COLUMN, dataType = DataType.DATE_STRING, canBeNull = false)
	public Date timeStamp;

	@DatabaseField(columnName = AVATAR_COLUMN, canBeNull = false)
	public String avatar;

	@DatabaseField(columnName = "type", canBeNull = false)
	public String type;

	protected LedgerDatabaseEntry(Date timeStamp, String avatar, String type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.type = type;
	}

	protected LedgerDatabaseEntry() {}
}
