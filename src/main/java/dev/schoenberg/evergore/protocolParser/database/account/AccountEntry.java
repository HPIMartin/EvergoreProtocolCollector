package dev.schoenberg.evergore.protocolParser.database.account;

import java.util.UUID;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

@DatabaseTable(tableName = AccountEntry.TABLE)
public class AccountEntry {
	public static final String TABLE = "account";
	public static final String NAME_KEY_COLUMN = "name_key";
	public static final String PASSWORD_HASH_COLUMN = "password_hash";
	public static final String PASSWORD_STATE_COLUMN = "password_state";
	public static final String LAST_LOGIN_COLUMN = "last_login_epoch_millis";

	@DatabaseField(columnName = "id", generatedId = true, canBeNull = false)
	public UUID id;

	@DatabaseField(columnName = "avatar", canBeNull = false)
	public String avatar;

	@DatabaseField(columnName = NAME_KEY_COLUMN, canBeNull = false)
	public String nameKey;

	@DatabaseField(columnName = PASSWORD_HASH_COLUMN, canBeNull = false)
	public String passwordHash;

	@DatabaseField(columnName = PASSWORD_STATE_COLUMN, dataType = DataType.ENUM_STRING, canBeNull = false)
	public PasswordState passwordState;

	@DatabaseField(columnName = "role", dataType = DataType.ENUM_STRING, canBeNull = false)
	public Role role;

	@DatabaseField(columnName = LAST_LOGIN_COLUMN)
	public Long lastLoginEpochMillis;

	public AccountEntry(String avatar, String nameKey, String passwordHash, PasswordState passwordState, Role role, Long lastLoginEpochMillis) {
		this.avatar = avatar;
		this.nameKey = nameKey;
		this.passwordHash = passwordHash;
		this.passwordState = passwordState;
		this.role = role;
		this.lastLoginEpochMillis = lastLoginEpochMillis;
	}

	protected AccountEntry() {}
}
