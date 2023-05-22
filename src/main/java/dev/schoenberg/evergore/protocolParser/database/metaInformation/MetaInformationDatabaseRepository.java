package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation.*;
import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.util.Optional.*;

import java.util.*;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class MetaInformationDatabaseRepository extends Repository<MetaInformationEntry> implements MetaInformationRepository {
	private final Dao<MetaInformationEntry, String> meta;

	public static MetaInformationDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		return new MetaInformationDatabaseRepository(con, logger, getDao(con, MetaInformationEntry.class));
	}

	private MetaInformationDatabaseRepository(ConnectionSource con, Logger logger, Dao<MetaInformationEntry, String> meta) {
		super(con, logger, MetaInformationEntry.class);
		this.meta = meta;
	}

	@Override
	public <T> Optional<T> get(MetaInformationKey<T> mik) {
		List<MetaInformationEntry> result = getAllFor(mik.id);

		if (result.isEmpty()) {
			return empty();
		}

		return of(convert(mik, result.get(0)).value);
	}

	@Override
	public <T> void add(List<MetaInformation<T>> meta) {
		meta.forEach(this::storeInformation);
	}

	private List<MetaInformationEntry> getAllFor(String key) {
		return silentThrow(() -> meta.queryBuilder().limit(1L).where().eq(KEY_COLUMN, key).query());
	}

	private <T> void storeInformation(MetaInformation<T> metainformation) {
		List<MetaInformationEntry> existing = getAllFor(metainformation.key.id);
		if (existing.isEmpty()) {
			silentThrow(() -> meta.create(convert(metainformation)));
		} else {
			silentThrow(() -> meta.update(existing.get(0).changeValue(metainformation.getSerializedValue())));
		}
	}

	private <T> MetaInformation<T> convert(MetaInformationKey<T> key, MetaInformationEntry dbEntry) {
		return fromSerializedValue(key, dbEntry.value);
	}

	private <T> MetaInformationEntry convert(MetaInformation<T> entry) {
		return new MetaInformationEntry(entry.key.id, entry.getSerializedValue());
	}
}
