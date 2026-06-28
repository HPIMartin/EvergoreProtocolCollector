package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.util.List;
import java.util.Optional;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.Repository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation.fromSerializedValue;
import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.KEY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class MetaInformationDatabaseRepository extends Repository<MetaInformationEntry> implements MetaInformationRepository {
	private final Dao<MetaInformationEntry, String> meta;

	public static MetaInformationDatabaseRepository get(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		ConnectionSource con = getCon(config, logger, hook);
		MetaInformationDatabaseRepository repository = new MetaInformationDatabaseRepository(con, logger, getDao(con, MetaInformationEntry.class));
		repository.ensureTable();
		return repository;
	}

	private MetaInformationDatabaseRepository(ConnectionSource con, Logger logger, Dao<MetaInformationEntry, String> meta) {
		super(con, logger, MetaInformationEntry.class);
		this.meta = meta;
	}

	@Override
	public <T> Optional<T> get(MetaInformationKey<T> key) {
		List<MetaInformationEntry> result = getAllFor(key.id);

		if (result.isEmpty()) {
			return empty();
		}

		return of(convert(key, result.get(0)).value());
	}

	@Override
	public <T> void add(List<MetaInformation<T>> meta) {
		meta.forEach(this::storeInformation);
	}

	private List<MetaInformationEntry> getAllFor(String key) {
		return silentThrow(() -> meta.queryBuilder().limit(1L).where().eq(KEY_COLUMN, key).query());
	}

	private <T> void storeInformation(MetaInformation<T> metainformation) {
		List<MetaInformationEntry> existing = getAllFor(metainformation.key().id);
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
		return new MetaInformationEntry(entry.key().id, entry.getSerializedValue());
	}
}
