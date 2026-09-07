package dev.schoenberg.evergore.protocolParser.database.bank;

import java.util.Date;
import java.util.UUID;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = BankDatabaseEntry.TABLE)
public class BankDatabaseEntry {
	public static final String TABLE = "bankEntries";
	public static final String AVATAR_COLUMN = "avatar";
	public static final String TIMESTAMP_COLUMN = "timeStamp";
	public static final String ID_COLUMN = "id";

	@DatabaseField(columnName = ID_COLUMN, generatedId = true, allowGeneratedIdInsert = true, canBeNull = false)
	public UUID id;

	@DatabaseField(columnName = TIMESTAMP_COLUMN, dataType = DataType.DATE_STRING, canBeNull = false)
	public Date timeStamp;

	@DatabaseField(columnName = AVATAR_COLUMN, canBeNull = false)
	public String avatar;

	@DatabaseField(columnName = "amount", canBeNull = false, throwIfNull = true)
	public int amount;

	@DatabaseField(columnName = "type", canBeNull = false)
	public String type;

	public BankDatabaseEntry(Date timeStamp, String avatar, int amount, String type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.amount = amount;
		this.type = type;
	}

	protected BankDatabaseEntry() {}
}
