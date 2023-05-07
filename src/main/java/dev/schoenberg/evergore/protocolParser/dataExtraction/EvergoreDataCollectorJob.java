package dev.schoenberg.evergore.protocolParser.dataExtraction;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;
import static dev.schoenberg.evergore.protocolParser.domain.Withdrawal.*;
import static java.util.stream.Collectors.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.dataExtraction.parser.*;
import dev.schoenberg.evergore.protocolParser.dataExtraction.website.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.database.storage.*;
import dev.schoenberg.evergore.protocolParser.domain.*;
import io.micronaut.scheduling.annotation.*;
import jakarta.inject.*;

@Singleton
public class EvergoreDataCollectorJob {
	private final Logger logger;
	private final PageContentExtractor extractor;
	private final BankDatabaseRepository bankRepo;
	private final StorageDatabaseRepository storageRepo;

	public EvergoreDataCollectorJob(Logger logger, PageContentExtractor extractor, BankDatabaseRepository bankRepo, StorageDatabaseRepository storageRepo) {
		this.logger = logger;
		this.extractor = extractor;
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
	}

	@Scheduled(fixedDelay = "24h", initialDelay = "30s")
	void scheduleEvery24Hours() {
		logger.info("Scheduled extraction started...");
		loadData();
		logger.info("Scheduled extraction finished!");
	}

	public void loadData() {
		PageContents load = extractor.load();
		updateBankEntries(EntityParser.parse(load.bank));
		updateLagerEntries(EntityParser.parse(load.lager));
	}

	private void updateLagerEntries(List<Entry> lager) {
		StorageEntry latest = storageRepo.getNewest();
		logger.debug("Latest element from: " + latest.timeStamp);
		AtomicInteger beforeFilter = new AtomicInteger(0);
		AtomicInteger afterFilter = new AtomicInteger(0);
		storageRepo.add(lager.stream().map(this::mapStorage).flatMap(List::stream).peek(x -> beforeFilter.incrementAndGet()).filter(e -> isNewer(latest, e))
				.peek(x -> afterFilter.incrementAndGet()).collect(toList()));
		logger.debug("before filter: " + beforeFilter.get());
		logger.debug("after filter: " + afterFilter.get());
	}

	private void updateBankEntries(List<Entry> bank) {
		BankEntry latest = bankRepo.getNewest();
		logger.debug("Latest element from: " + latest.timeStamp);
		AtomicInteger beforeFilter = new AtomicInteger(0);
		AtomicInteger afterFilter = new AtomicInteger(0);
		bankRepo.add(bank.stream().map(this::mapBank).flatMap(List::stream).peek(x -> beforeFilter.incrementAndGet()).filter(e -> isNewer(latest, e))
				.peek(x -> afterFilter.incrementAndGet()).collect(toList()));
		logger.debug("before filter: " + beforeFilter.get());
		logger.debug("after filter: " + afterFilter.get());
	}

	private boolean isNewer(BankEntry latest, BankEntry toCheck) {
		return toCheck.timeStamp.isAfter(latest.timeStamp);
	}

	private boolean isNewer(StorageEntry latest, StorageEntry toCheck) {
		return toCheck.timeStamp.isAfter(latest.timeStamp);
	}

	private List<BankEntry> mapBank(Entry e) {
		Instant time = e.date;
		String avatar = e.avatar;
		TransferType type = ENTNAHME.equals(e.getType()) ? Entnahme : Einlagerung;
		return e.items.stream().map(i -> new BankEntry(time, avatar, i.quantity, type)).collect(toList());
	}

	private List<StorageEntry> mapStorage(Entry e) {
		Instant time = e.date;
		String avatar = e.avatar;
		TransferType type = ENTNAHME.equals(e.getType()) ? Entnahme : Einlagerung;
		return e.items.stream().map(i -> new StorageEntry(time, avatar, i.quantity, i.name, i.quality, type)).collect(toList());
	}
}
