package dev.schoenberg.evergore.protocolParser;

import java.time.*;

import jakarta.inject.*;

import io.micronaut.context.annotation.Factory;

import dev.schoenberg.evergore.protocolParser.application.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.dataExtraction.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.*;
import dev.schoenberg.evergore.protocolParser.database.storage.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.*;
import dev.schoenberg.evergore.protocolParser.helper.selenium.*;

@Factory
public class ApplicationFactory {
	@Singleton
	public FileLoader fileLoader(Configuration config, Logger logger) {
		return new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader(), logger);
	}

	@Singleton
	public EvergoreDataExtractor evergoreDataExtractor(PageSource pageSource, BankRepository bankRepo, StorageRepository storageRepo, Logger logger) {
		return new EvergoreDataExtractor(pageSource, bankRepo, storageRepo, logger);
	}

	@Singleton
	public EvergoreDataEvaluator evergoreDataEvaluator(MetaInformationRepository metaRepo, StorageRepository storageRepo, BankRepository bankRepo, Logger logger) {
		return new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, logger);
	}

	@Singleton
	public LastRunStatus lastRunStatus() {
		return new LastRunStatus();
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

	@Singleton
	public Clock clock() {
		return Clock.systemUTC();
	}
}
