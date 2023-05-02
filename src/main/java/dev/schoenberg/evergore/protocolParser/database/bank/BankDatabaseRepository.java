package dev.schoenberg.evergore.protocolParser.database.bank;

import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.sql.Timestamp.*;
import static java.util.stream.Collectors.*;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class BankDatabaseRepository extends Repository<BankDatabaseEntry> implements BankRepository {
	private final Dao<BankDatabaseEntry, String> bank;

	public static BankDatabaseRepository get(Configuration config, Logger logger) {
		ConnectionSource con = getCon(config);
		return new BankDatabaseRepository(con, logger, getDao(con, BankDatabaseEntry.class));
	}

	private BankDatabaseRepository(ConnectionSource con, Logger logger, Dao<BankDatabaseEntry, String> bank) {
		super(con, logger, BankDatabaseEntry.class);
		this.bank = bank;
	}

	public List<BankEntry> getAllFor(String avatar) {
		List<BankDatabaseEntry> result = silentThrow(
				() -> bank.queryBuilder().where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return convert(result);
	}

	@Override
	public void add(List<BankEntry> newEntries) {
		silentThrow(() -> bank.create(newEntries.stream().map(this::convert).collect(toList())));
	}

	@Override
	public BankEntry getNewest() {
		return convert(silentThrow(() -> {
			GenericRawResults<String[]> raw = bank.queryRaw("SELECT max(" + TIMESTAMP_COLUMN + ") FROM " + TABLE);

			List<String[]> results = raw.getResults();

			log(results);

			if (results.isEmpty() || results.get(0) == null || results.get(0)[0] == null) {
				return new BankDatabaseEntry(new Date(Long.MIN_VALUE), "", 0, convert(TransferType.Einlagerung));
			}

			Timestamp highestTimeStamp = valueOf(results.get(0)[0]);
			return bank.queryBuilder().where().eq(BankDatabaseEntry.TIMESTAMP_COLUMN, highestTimeStamp).queryForFirst();
		}));
	}

	private void log(List<String[]> results) {
		boolean first_x = true;
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String[] x : results) {
			if (!first_x) {
				sb.append(",");
			}
			first_x = false;
			boolean first_y = true;
			sb.append("[");
			for (String y : x) {
				if (!first_y) {
					sb.append(",");
				}
				sb.append(y);
				first_y = false;
			}
			sb.append("]");
		}
		sb.append("]");

		logger.debug(sb.toString());
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
