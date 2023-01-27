package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.*;

import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

@DatabaseTable(tableName = "storageEntries")
public class StorageDatabaseEntry {
	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	public UUID id;

	@DatabaseField(columnName = "timeStamp", dataType = DataType.DATE_STRING)
	public Date timeStamp;

	@DatabaseField(columnName = "account")
	public String account;

	@DatabaseField(columnName = "amount")
	public int amount;

	@DatabaseField(columnName = "item")
	public List<ItemDatabase> items;

	@DatabaseField(columnName = "type")
	public String type;

	public StorageDatabaseEntry(Date timeStamp, String account, int amount, List<ItemDatabase> items, String type) {
		this.timeStamp = timeStamp;
		this.account = account;
		this.amount = amount;
		this.items = items;
		this.type = type;
	}

	protected StorageDatabaseEntry() {
		// ORMLite needs a no-arg constructor
	}
}
