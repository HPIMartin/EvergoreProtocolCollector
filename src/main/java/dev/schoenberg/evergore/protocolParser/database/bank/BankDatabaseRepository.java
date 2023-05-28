package dev.schoenberg.evergore.protocolParser.database.bank;

import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.AVATAR_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry.TABLE;
import static dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseEntry.TIMESTAMP_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.sql.Timestamp.*;
import static java.util.stream.Collectors.*;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Date;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class BankDatabaseRepository extends Repository<BankDatabaseEntry> implements BankRepository {
	private final Dao<BankDatabaseEntry, String> bank;

	private final TransferTypeDatabaseVisitor transferTypeVisitor = new TransferTypeDatabaseVisitor();

	public static BankDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		return new BankDatabaseRepository(con, logger, getDao(con, BankDatabaseEntry.class));
	}

	private BankDatabaseRepository(ConnectionSource con, Logger logger, Dao<BankDatabaseEntry, String> bank) {
		super(con, logger, BankDatabaseEntry.class);
		this.bank = bank;
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, long page, long size) {
		List<BankDatabaseEntry> result = silentThrow(
				() -> bank.queryBuilder().orderBy(TIMESTAMP_COLUMN, false).limit(size).offset(page * size).where().eq(AVATAR_COLUMN, avatar).query());

		if (result.isEmpty()) {
			throw new NoElementFound(avatar);
		}

		return convert(result);
	}

	@Override
	public List<BankEntry> getAllFor(String avatar, LocalDateTime after) {
		Timestamp afterTimestamp = Timestamp.from(after.atZone(Constants.APP_ZONE).toInstant());

		List<BankDatabaseEntry> result = silentThrow(
				() -> bank.queryBuilder().where().eq(AVATAR_COLUMN, avatar).and().gt(TIMESTAMP_COLUMN, afterTimestamp).query());

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
				return new BankDatabaseEntry(new Date(Long.MIN_VALUE), "", 0, transferTypeVisitor.convert(TransferType.Einlagerung));
			}

			Timestamp highestTimeStamp = valueOf(results.get(0)[0]);
			return bank.queryBuilder().where().eq(BankDatabaseEntry.TIMESTAMP_COLUMN, highestTimeStamp).queryForFirst();
		}));
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		List<BankDatabaseEntry> avatars = silentThrow(() -> bank.queryBuilder().distinct().selectColumns(AVATAR_COLUMN).query());
		return avatars.stream().map(bde -> bde.avatar).collect(toList());
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
		return new BankEntry(dbEntry.timeStamp.toInstant(), dbEntry.avatar, dbEntry.amount, transferTypeVisitor.convert(dbEntry.type));
	}

	private BankDatabaseEntry convert(BankEntry entry) {
		return new BankDatabaseEntry(from(entry.timeStamp), entry.avatar, entry.amount, transferTypeVisitor.convert(entry.type));
	}
}
