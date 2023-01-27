package dev.schoenberg.evergore.protocolParser.database.bank;

import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.sql.Timestamp.*;
import static java.util.stream.Collectors.*;

import java.sql.*;
import java.util.*;

import com.j256.ormlite.dao.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.entity.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class BankDatabaseRepository extends Repository implements BankRepository {
	private final Dao<BankDatabaseEntry, String> bank;

	public static BankDatabaseRepository get(Configuration config) {
		return new BankDatabaseRepository(getDao(config, BankDatabaseEntry.class));
	}

	private BankDatabaseRepository(Dao<BankDatabaseEntry, String> bank) {
		this.bank = bank;
	}

	public List<BankEntry> getAllFor(String avatar) {
		return convert(silentThrow(() -> bank.queryBuilder().where().eq(AVATAR_COLUMN, avatar).query()));
	}

	@Override
	public void add(List<BankEntry> newEntries) {
		silentThrow(() -> bank.create(newEntries.stream().map(this::convert).collect(toList())));
	}

	@Override
	public BankEntry getNewest() {
		return convert(silentThrow(() -> {
			GenericRawResults<String[]> raw = bank.queryRaw("SELECT max(" + TIMESTAMP_COLUMN + ") FROM " + TABLE);
			Timestamp maxValue = valueOf(raw.getFirstResult()[0]);
			return bank.queryBuilder().where().eq(BankDatabaseEntry.TIMESTAMP_COLUMN, maxValue).queryForFirst();
		}));
	}

	private List<BankEntry> convert(List<BankDatabaseEntry> dbEntries) {
		return dbEntries.stream().map(this::convert).collect(toList());
	}

	private BankEntry convert(BankDatabaseEntry dbEntry) {
		return new BankEntry(dbEntry.timeStamp.toInstant(), dbEntry.avatar, dbEntry.amount, convert(dbEntry.type));
	}

	private BankDatabaseEntry convert(BankEntry entry) {
		return new BankDatabaseEntry(from(entry.timeStamp), entry.avatar, entry.amount, convert(entry.type));
	}

	// TODO: Visitor?
	private TransferType convert(String type) {
		if ("Entnahme".equals(type)) {
			return TransferType.Entnahme;
		}
		if ("Einlagerung".equals(type)) {
			return TransferType.Einlagerung;
		}

		throw new RuntimeException("Lazy basdard...");
	}

	private String convert(TransferType type) {
		if (TransferType.Entnahme.equals(type)) {
			return "Entnahme";
		}
		if (TransferType.Einlagerung.equals(type)) {
			return "Einlagerung";
		}

		throw new RuntimeException("Lazy basdard...");
	}
}
