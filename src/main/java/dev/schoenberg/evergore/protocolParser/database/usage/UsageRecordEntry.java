package dev.schoenberg.evergore.protocolParser.database.usage;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

@DatabaseTable(tableName = "usage_record")
public class UsageRecordEntry {
	@DatabaseField(columnName = "id", generatedId = true)
	public Long id;

	@DatabaseField(columnName = "account_key", canBeNull = false)
	public String accountKey;

	@DatabaseField(columnName = "at_epoch_millis", canBeNull = false)
	public long atEpochMillis;

	@DatabaseField(columnName = "what", dataType = DataType.ENUM_STRING, canBeNull = false)
	public UsageKind what;

	@DatabaseField(columnName = "detail")
	public String detail;

	public UsageRecordEntry(String accountKey, long atEpochMillis, UsageKind what, String detail) {
		this.accountKey = accountKey;
		this.atEpochMillis = atEpochMillis;
		this.what = what;
		this.detail = detail;
	}

	protected UsageRecordEntry() {}
}
