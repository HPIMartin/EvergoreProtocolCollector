package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.TRANSFER_TYPE_WORDS;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class StoredAvatarReconciliationTest {
	private static final Path FIXTURE = Paths.get("src/test/resources/testdata.sqlite");
	private static final Path WORKING_DIRECTORY = Paths.get("build/tmp/reconciliation");

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "Entnahmefreund", "Einzahlungsmeister", "Freund der Einlagerung"})
	void flagsAnAvatarAMisreadTypeWordCouldHaveProduced(String avatar) {
		boolean flagged = misreadTypeWordSuspect(avatar);

		assertThat(flagged).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Aurora", "Hans Meyer", "Entnahmslos", "Einlagern"})
	void acceptsAnOrdinaryAvatarName(String avatar) {
		boolean flagged = misreadTypeWordSuspect(avatar);

		assertThat(flagged).isFalse();
	}

	@Test
	void noAvatarStoredInTheFixtureCouldHaveComeFromAMisreadTypeWord() {
		List<String> stored = avatarsIn(FIXTURE);

		assertThat(stored).isNotEmpty();
		assertThat(stored).noneMatch(StoredAvatarReconciliationTest::misreadTypeWordSuspect);
	}

	private static boolean misreadTypeWordSuspect(String avatar) {
		return avatar.isBlank() || TRANSFER_TYPE_WORDS.stream().anyMatch(avatar::contains);
	}

	private static List<String> avatarsIn(Path database) {
		Configuration config = new ReconciliationConfiguration(copyToWorkingDirectory(database));
		LoggerSpy logger = new LoggerSpy();
		BankDatabaseRepository bank = BankDatabaseRepository.get(config, logger, () -> {});
		StorageDatabaseRepository storage = StorageDatabaseRepository.get(config, logger, () -> {});
		return Stream.concat(bank.getAllDifferentAvatars().stream(), storage.getAllDifferentAvatars().stream()).distinct().toList();
	}

	private static Path copyToWorkingDirectory(Path database) {
		Path target = WORKING_DIRECTORY.resolve(database.getFileName());
		return silentThrow(() -> {
			Files.createDirectories(WORKING_DIRECTORY);
			Files.deleteIfExists(target);
			Files.copy(database, target);
			return target;
		});
	}

	private static class ReconciliationConfiguration extends Configuration {
		private final Path database;

		ReconciliationConfiguration(Path database) {
			this.database = database;
		}

		@Override
		public String getDatabasePath() {
			return database.toString();
		}
	}
}
