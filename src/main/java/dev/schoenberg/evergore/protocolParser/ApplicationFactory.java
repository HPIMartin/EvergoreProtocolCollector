package dev.schoenberg.evergore.protocolParser;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.AlternativeFileLoaderWrapper;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.DiscFileLoader;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.ResourceFileLoader;
import dev.schoenberg.evergore.protocolParser.helper.selenium.FileLoader;

@Factory
public class ApplicationFactory {
	@Singleton
	public FileLoader fileLoader(Configuration config) {
		return new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader());
	}

	@Singleton
	public BankDatabaseRepository bankDatabaseRepository(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		return BankDatabaseRepository.get(config, logger, hook);
	}

	@Singleton
	public StorageDatabaseRepository storageDatabaseRepository(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		return StorageDatabaseRepository.get(config, logger, hook);
	}

	@Singleton
	public MetaInformationDatabaseRepository metaInformationDatabaseRepository(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		return MetaInformationDatabaseRepository.get(config, logger, hook);
	}

	@Singleton
	public PreDatabaseConnectionHook preDatabaseConnectionHook() {
		return () -> {};
	}

	@Singleton
	public PostCollectionHook postCollectionHook() {
		return () -> {};
	}
}
