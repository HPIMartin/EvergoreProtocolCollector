package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.util.List;

import com.j256.ormlite.dao.Dao;

import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;

import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.KEY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.stream.Collectors.toMap;

public class MetaInformationDatabaseRepository implements MetaInformationRepository {
	private final SqliteDatabase database;

	public MetaInformationDatabaseRepository(SqliteDatabase database) {
		this.database = database;
	}

	@Override
	public MetaInformationSnapshot snapshot() {
		List<MetaInformationEntry> all = silentThrow(() -> meta().queryForAll());

		return new MetaInformationSnapshot(all.stream().collect(toMap(entry -> entry.key, entry -> entry.value)));
	}

	@Override
	public void add(List<? extends MetaInformation<?>> meta) {
		database.inTransaction(() -> {
			meta.forEach(this::storeInformation);
			return null;
		});
	}

	private List<MetaInformationEntry> getAllFor(String key) {
		return silentThrow(() -> meta().queryBuilder().limit(1L).where().eq(KEY_COLUMN, key).query());
	}

	private <T> void storeInformation(MetaInformation<T> metainformation) {
		List<MetaInformationEntry> existing = getAllFor(metainformation.key().id);
		if (existing.isEmpty()) {
			silentThrow(() -> meta().create(convert(metainformation)));
		} else {
			silentThrow(() -> meta().update(existing.get(0).changeValue(metainformation.getSerializedValue())));
		}
	}

	private Dao<MetaInformationEntry, String> meta() {
		return database.dao(MetaInformationEntry.class);
	}

	private <T> MetaInformationEntry convert(MetaInformation<T> entry) {
		return new MetaInformationEntry(entry.key().id, entry.getSerializedValue());
	}
}
