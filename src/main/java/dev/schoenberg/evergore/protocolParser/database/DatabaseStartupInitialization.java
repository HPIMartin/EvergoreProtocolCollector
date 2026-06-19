package dev.schoenberg.evergore.protocolParser.database;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;

import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Path.of;

@Singleton
public class DatabaseStartupInitialization implements ApplicationEventListener<ServerStartupEvent> {
	private final Configuration config;
	private final StorageDatabaseRepository storage;
	private final BankDatabaseRepository bank;
	private final MetaInformationDatabaseRepository meta;

	public DatabaseStartupInitialization(Configuration config, StorageDatabaseRepository storage, BankDatabaseRepository bank, MetaInformationDatabaseRepository meta) {
		this.config = config;
		this.storage = storage;
		this.bank = bank;
		this.meta = meta;
	}

	@Override
	public void onApplicationEvent(ServerStartupEvent event) {
		silentThrow(() -> createDirectories(of(config.getDatabasePath()).getParent()));

		storage.init();
		bank.init();
		meta.init();
	}
}
