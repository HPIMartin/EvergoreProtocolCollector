package dev.schoenberg.evergore.protocolParser.database.storage;

import java.util.*;

import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

@DatabaseTable(tableName = "storageEntries")
public class ItemDatabase {
	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	public UUID id;

	@DatabaseField(columnName = "quantity")
	public int quantity;

	@DatabaseField(columnName = "name")
	public String name;

	@DatabaseField(columnName = "quality")
	public int quality;

	public ItemDatabase(int quantity, String name, int quality) {
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
	}

	protected ItemDatabase() {
		// ORMLite needs a no-arg constructor
	}
}
