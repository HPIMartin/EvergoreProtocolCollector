package dev.schoenberg.evergore.protocolParser.dataExtraction;

import static dev.schoenberg.evergore.protocolParser.businessLogic.entity.TransferType.*;
import static dev.schoenberg.evergore.protocolParser.domain.Withdrawal.*;
import static java.util.stream.Collectors.*;

import java.time.*;
import java.util.*;

import org.slf4j.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.entity.*;
import dev.schoenberg.evergore.protocolParser.dataExtraction.parser.*;
import dev.schoenberg.evergore.protocolParser.dataExtraction.website.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.domain.*;
import io.micronaut.scheduling.annotation.*;
import jakarta.inject.*;

@Singleton
public class EvergoreDataCollectorJob {
	private static final Logger LOG = LoggerFactory.getLogger(EvergoreDataCollectorJob.class);

	private final PageContentExtractor extractor;

	private BankDatabaseRepository bankRepo;

	public EvergoreDataCollectorJob(PageContentExtractor extractor, BankDatabaseRepository bankRepo) {
		this.extractor = extractor;
		this.bankRepo = bankRepo;
	}

	@Scheduled(fixedDelay = "24h", initialDelay = "1m")
	void scheduleEvery24Hours() {
		loadData();
	}

	public void loadData() {
		PageContents load = extractor.load();
		List<Entry> bank = EntityParser.parse(load.bank);
		List<Entry> lager = EntityParser.parse(load.lager);
		BankEntry latest = bankRepo.getNewest();
		bankRepo.add(
				bank.stream().map(this::map).flatMap(List::stream).filter(e -> isNewer(latest, e)).collect(toList()));
	}

	private boolean isNewer(BankEntry latest, BankEntry toCheck) {
		return toCheck.timeStamp.isAfter(latest.timeStamp);
	}

	private List<BankEntry> map(Entry e) {
		Instant time = e.date;
		String avatar = e.avatar;
		TransferType type = e.getType().equals(ENTNAHME) ? Entnahme : Einlagerung;
		return e.items.stream().map(i -> new BankEntry(time, avatar, i.quantity, type)).collect(toList());
	}
}
