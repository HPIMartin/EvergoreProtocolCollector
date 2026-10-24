package dev.schoenberg.evergore.protocolParser;

import java.security.SecureRandom;
import java.time.Clock;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.openqa.selenium.support.ui.Sleeper;

import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.application.CreateAccounts;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataEvaluator;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.IssuedPasswords;
import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.application.PasswordGenerator;
import dev.schoenberg.evergore.protocolParser.application.PasswordHasher;
import dev.schoenberg.evergore.protocolParser.businessLogic.Constants;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntityParser;
import dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntryFactory;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.account.AccountDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.config.PasswordHashConfiguration;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.AlternativeFileLoaderWrapper;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.DiscFileLoader;
import dev.schoenberg.evergore.protocolParser.helper.fileLoader.ResourceFileLoader;
import dev.schoenberg.evergore.protocolParser.helper.security.Argon2idPasswordHasher;
import dev.schoenberg.evergore.protocolParser.helper.security.RandomPasswordGenerator;
import dev.schoenberg.evergore.protocolParser.helper.selenium.FileLoader;

@Factory
public class ApplicationFactory {
	@Singleton
	public FileLoader fileLoader(Configuration config, Logger logger) {
		return new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader(), logger);
	}

	@Singleton
	public EvergoreDataExtractor evergoreDataExtractor(PageSource pageSource, BankRepository bankRepo, StorageRepository storageRepo, EntityParser parser) {
		return new EvergoreDataExtractor(pageSource, bankRepo, storageRepo, parser);
	}

	@Singleton
	public EntityParser entityParser(Logger logger) {
		return new EntityParser(new EntryFactory(logger));
	}

	@Singleton
	public KnownAvatars knownAvatars(BankRepository bankRepo, StorageRepository storageRepo) {
		return new KnownAvatars(bankRepo, storageRepo);
	}

	@Singleton
	public AvatarContributions avatarContributions(KnownAvatars knownAvatars, MetaInformationRepository metaRepo, BankRepository bankRepo, StorageRepository storageRepo) {
		return new AvatarContributions(knownAvatars, metaRepo, bankRepo, storageRepo);
	}

	@Singleton
	public EvergoreDataEvaluator evergoreDataEvaluator(MetaInformationRepository metaRepo, StorageRepository storageRepo, BankRepository bankRepo, KnownAvatars knownAvatars,
			Clock clock, Logger logger) {
		return new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, knownAvatars, clock, logger);
	}

	@Singleton
	public LastRunStatus lastRunStatus() {
		return new LastRunStatus();
	}

	@Singleton
	@Bean(preDestroy = "close")
	public SqliteDatabase sqliteDatabase(Configuration config, PreDatabaseConnectionHook hook, Logger logger) {
		return SqliteDatabase.open(config, hook, logger);
	}

	@Singleton
	public BankDatabaseRepository bankDatabaseRepository(SqliteDatabase database) {
		return new BankDatabaseRepository(database);
	}

	@Singleton
	public StorageDatabaseRepository storageDatabaseRepository(SqliteDatabase database) {
		return new StorageDatabaseRepository(database);
	}

	@Singleton
	public MetaInformationDatabaseRepository metaInformationDatabaseRepository(SqliteDatabase database) {
		return new MetaInformationDatabaseRepository(database);
	}

	@Singleton
	public AccountDatabaseRepository accountDatabaseRepository(SqliteDatabase database) {
		return new AccountDatabaseRepository(database);
	}

	@Singleton
	public PasswordHasher passwordHasher(PasswordHashConfiguration configuration) {
		return new Argon2idPasswordHasher(configuration, new SecureRandom());
	}

	@Singleton
	public PasswordGenerator passwordGenerator() {
		return new RandomPasswordGenerator(new SecureRandom());
	}

	@Singleton
	public IssuedPasswords issuedPasswords() {
		return (avatar, password) -> {};
	}

	@Singleton
	public CreateAccounts createAccounts(KnownAvatars knownAvatars, AccountRepository accounts, PasswordGenerator passwordGenerator, PasswordHasher passwordHasher,
			IssuedPasswords issuedPasswords, Logger logger) {
		return new CreateAccounts(knownAvatars, accounts, passwordGenerator, passwordHasher, issuedPasswords, logger);
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
		return Clock.system(Constants.APP_ZONE);
	}

	@Singleton
	public Sleeper sleeper() {
		return Sleeper.SYSTEM_SLEEPER;
	}
}
