package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KRISTALL;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KUPFERERZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGISCHE_AETHERBINDE;
import static java.util.Arrays.asList;

public class TestDataGenerator {
	private static final String TARGET = "src/test/resources/testdata.sqlite";

	public static void main(String[] args) throws Exception {
		Files.deleteIfExists(Paths.get(TARGET));

		LoggerSpy logger = new LoggerSpy();
		Configuration config = new FixtureConfiguration();

		BankDatabaseRepository bank = BankDatabaseRepository.get(config, logger, () -> {});
		bank.add(auroraBankEntries());
		bank.add(boreasBankEntries());
		bank.add(calixBankEntries());

		StorageDatabaseRepository storage = StorageDatabaseRepository.get(config, logger, () -> {});
		storage.add(auroraStorageEntries());
		storage.add(boreasStorageEntries());
	}

	private static List<BankEntry> auroraBankEntries() {
		return asList(new BankEntry(ts(2024, 1, 10, 10, 0), "Aurora", 1000, EINLAGERUNG), new BankEntry(ts(2024, 1, 11, 11, 0), "Aurora", 500, EINLAGERUNG),
				new BankEntry(ts(2024, 1, 12, 12, 0), "Aurora", 200, ENTNAHME));
	}

	private static List<BankEntry> boreasBankEntries() {
		return asList(new BankEntry(ts(2024, 2, 1, 9, 0), "Boreas", 750, EINLAGERUNG));
	}

	private static List<BankEntry> calixBankEntries() {
		return asList(new BankEntry(ts(2024, 3, 1, 8, 0), "Calix", 300, ENTNAHME));
	}

	private static List<StorageEntry> auroraStorageEntries() {
		return asList(new StorageEntry(ts(2024, 1, 15, 10, 0), "Aurora", 10, KUPFERERZ.ingameName, 100, EINLAGERUNG),
				new StorageEntry(ts(2024, 1, 16, 11, 0), "Aurora", 2, MAGISCHE_AETHERBINDE.ingameName, 100, EINLAGERUNG),
				new StorageEntry(ts(2024, 1, 17, 12, 0), "Aurora", 1, KRISTALL.ingameName, 100, ENTNAHME));
	}

	private static List<StorageEntry> boreasStorageEntries() {
		return asList(new StorageEntry(ts(2024, 2, 5, 9, 0), "Boreas", 1, MAGISCHE_AETHERBINDE.ingameName, 50, EINLAGERUNG));
	}

	private static Instant ts(int year, int month, int day, int hour, int minute) {
		return LocalDateTime.of(year, month, day, hour, minute).atZone(APP_ZONE).toInstant();
	}

	private static class FixtureConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return TARGET;
		}
	}
}
