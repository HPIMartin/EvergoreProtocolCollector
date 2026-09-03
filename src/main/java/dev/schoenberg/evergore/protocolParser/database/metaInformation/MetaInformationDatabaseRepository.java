package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.Repository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.KEY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.stream.Collectors.toMap;

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
	public MetaInformationSnapshot snapshot() {
		List<MetaInformationEntry> all = silentThrow(() -> meta.queryForAll());

		return new MetaInformationSnapshot(all.stream().filter(entry -> entry.value != null).collect(toMap(entry -> entry.key, entry -> entry.value)));
	}

	@Override
	public void add(List<? extends MetaInformation<?>> meta) {
		inTransaction(() -> {
			meta.forEach(this::storeInformation);
			return null;
		});
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

	private <T> MetaInformationEntry convert(MetaInformation<T> entry) {
		return new MetaInformationEntry(entry.key().id, entry.getSerializedValue());
	}
}
