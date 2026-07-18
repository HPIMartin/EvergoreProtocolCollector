package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntityParser;
import dev.schoenberg.evergore.protocolParser.domain.Entry;

public class EvergoreDataExtractor {
	private final PageSource pageSource;
	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final Logger logger;

	public EvergoreDataExtractor(PageSource pageSource, BankRepository bankRepo, StorageRepository storageRepo, Logger logger) {
		this.pageSource = pageSource;
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.logger = logger;
	}

	public void loadData() {
		PageContents load = pageSource.load();
		updateBankEntries(EntityParser.parse(load.bank()));
		updateLagerEntries(EntityParser.parse(load.lager()));
	}

	private void updateLagerEntries(List<Entry> lager) {
		List<StorageEntry> parsed = lager.stream().map(this::mapStorage).flatMap(List::stream).toList();
		storageRepo.add(selectEntriesToIngest(parsed, StorageEntry::timeStamp, storageRepo::getAllSince));
	}

	private void updateBankEntries(List<Entry> bank) {
		List<BankEntry> parsed = bank.stream().map(this::mapBank).flatMap(List::stream).toList();
		bankRepo.add(selectEntriesToIngest(parsed, BankEntry::timeStamp, bankRepo::getAllSince));
	}

	// Dedup covers the whole scraped window (not just its newest minute), so a still-visible entry
	// that an earlier, buggy scrape failed to store gets healed permanently instead of staying lost.
	private <T> List<T> selectEntriesToIngest(List<T> parsed, Function<T, Instant> timestampOf, Function<Instant, List<T>> allSince) {
		if (parsed.isEmpty()) {
			return List.of();
		}
		Instant minParsed = parsed.stream().map(timestampOf).min(Comparator.naturalOrder()).orElseThrow();
		List<T> stored = allSince.apply(minParsed);
		return sortedByTimestamp(surplusOverStored(parsed, stored), timestampOf);
	}

	// Pages are scraped newest-first; ORMLite's create(Collection) on SQLite has no transaction, so a mid-batch
	// failure commits a partial batch. Ascending order keeps a partial commit a prefix of the oldest rows, so the
	// next run re-ingests the rest instead of permanently stranding everything below the new stored max.
	private <T> List<T> sortedByTimestamp(List<T> entries, Function<T, Instant> timestampOf) {
		return entries.stream().sorted(Comparator.comparing(timestampOf)).toList();
	}

	private <T> List<T> surplusOverStored(List<T> scraped, List<T> stored) {
		Map<T, Long> remainingStored = new HashMap<>(stored.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
		List<T> surplus = new ArrayList<>();
		for (T entry : scraped) {
			long remaining = remainingStored.getOrDefault(entry, 0L);
			if (remaining > 0) {
				remainingStored.put(entry, remaining - 1);
			} else {
				surplus.add(entry);
			}
		}
		return surplus;
	}

	private List<BankEntry> mapBank(Entry e) {
		Instant time = e.date();
		String avatar = e.avatar();
		TransferType type = e.type();
		return e.items().stream().map(i -> new BankEntry(time, avatar, i.quantity(), type)).toList();
	}

	private List<StorageEntry> mapStorage(Entry e) {
		Instant time = e.date();
		String avatar = e.avatar();
		TransferType type = e.type();
		return e.items().stream().map(i -> new StorageEntry(time, avatar, i.quantity(), i.name(), i.quality(), type)).toList();
	}
}
