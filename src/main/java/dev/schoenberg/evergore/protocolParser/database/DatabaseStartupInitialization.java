package dev.schoenberg.evergore.protocolParser.database;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.nio.file.Files.*;
import static java.nio.file.Path.*;

import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.database.storage.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import io.micronaut.context.event.*;
import io.micronaut.runtime.server.event.*;
import jakarta.inject.*;

@Singleton
public class DatabaseStartupInitialization implements ApplicationEventListener<ServerStartupEvent> {
	private final Configuration config;
	private final StorageDatabaseRepository storage;
	private final BankDatabaseRepository bank;

	public DatabaseStartupInitialization(Configuration config, StorageDatabaseRepository storage,
			BankDatabaseRepository bank) {
		this.config = config;
		this.storage = storage;
		this.bank = bank;
	}

	@Override
	public void onApplicationEvent(ServerStartupEvent event) {
		silentThrow(() -> createDirectories(of(config.getDatabasePath()).getParent()));

		storage.init();
		bank.init();
	}
}
