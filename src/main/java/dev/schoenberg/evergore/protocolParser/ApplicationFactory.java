package dev.schoenberg.evergore.protocolParser;

import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.database.storage.*;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.*;
import dev.schoenberg.evergore.protocolParser.helper.selenium.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;

@Factory
public class ApplicationFactory {
	@Singleton
	public FileLoader fileLoader(Configuration config) {
		return new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader());
	}

	@Singleton
	public BankDatabaseRepository bankDatabaseRepository(Configuration config, Logger logger) {
		return BankDatabaseRepository.get(config, logger);
	}

	@Singleton
	public StorageDatabaseRepository storageDatabaseRepository(Configuration config, Logger logger) {
		return StorageDatabaseRepository.get(config, logger);
	}
}