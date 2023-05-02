package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.util.stream.Collectors.*;

import java.util.*;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class MetaInformationDatabaseRepository extends Repository<MetaInformationEntry>
		implements MetaInformationRepository {
	private final Dao<MetaInformationEntry, String> bank;

	public static MetaInformationDatabaseRepository get(Configuration config, Logger logger) {
		ConnectionSource con = getCon(config);
		return new MetaInformationDatabaseRepository(con, logger, getDao(con, MetaInformationEntry.class));
	}

	private MetaInformationDatabaseRepository(ConnectionSource con, Logger logger,
			Dao<MetaInformationEntry, String> bank) {
		super(con, logger, MetaInformationEntry.class);
		this.bank = bank;
	}

	@Override
	public List<MetaInformation> get(String key) {
		List<MetaInformationEntry> result = silentThrow(() -> bank.queryBuilder().where().eq(KEY_COLUMN, key).query());

		if (result.isEmpty()) {
			throw new NoElementFound(key);
		}

		return convert(result);
	}

	@Override
	public void add(List<MetaInformation> meta) {
		silentThrow(() -> bank.create(meta.stream().map(this::convert).collect(toList())));
	}

	private List<MetaInformation> convert(List<MetaInformationEntry> dbEntries) {
		return dbEntries.stream().map(this::convert).collect(toList());
	}

	private MetaInformation convert(MetaInformationEntry dbEntry) {
		return new MetaInformation(dbEntry.key, dbEntry.value);
	}

	private MetaInformationEntry convert(MetaInformation entry) {
		return new MetaInformationEntry(entry.key, entry.value);
	}
}
