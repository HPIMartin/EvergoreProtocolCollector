package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.util.*;

import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

@DatabaseTable(tableName = MetaInformationEntry.TABLE)
public class MetaInformationEntry {
	public static final String TABLE = "metaInformation";
	public static final String KEY_COLUMN = "key";

	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	public UUID id;

	@DatabaseField(columnName = KEY_COLUMN)
	public String key;

	@DatabaseField(columnName = "value")
	public String value;

	public MetaInformationEntry(String key, String value) {
		this.key = key;
		this.value = value;
	}

	protected MetaInformationEntry() {
		// ORMLite needs a no-arg constructor
	}
}
