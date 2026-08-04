package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.validation.Validated;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.BankEntryView;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.EntryPage;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.StorageEntryView;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.TransferTypeWireNames;

import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.DEFAULT_PAGE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.DEFAULT_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.MAX_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.PAGE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.SIZE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Validated
@Controller(AvatarEntriesController.PATH)
public class AvatarEntriesController {
	public static final String PATH = "/api/v1/avatars/{" + AvatarEntriesController.PATH_VAR_AVATAR + "}";
	public static final String PATH_VAR_AVATAR = "avatar";

	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final TransferTypeWireNames wireNames;
	private final Logger logger;

	public AvatarEntriesController(BankRepository bankRepo, StorageRepository storageRepo, TransferTypeWireNames wireNames, Logger logger) {
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.wireNames = wireNames;
		this.logger = logger;
	}

	@Get("/bank")
	@Produces(APPLICATION_JSON)
	public EntryPage<BankEntryView> bankEntries(@PathVariable(PATH_VAR_AVATAR) String avatar, @QueryValue(value = PAGE, defaultValue = DEFAULT_PAGE) @Min(0) int page,
			@QueryValue(value = SIZE, defaultValue = DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		PageRequest window = new PageRequest(page, size);
		return pageOf(avatar, window, () -> bankRepo.countFor(avatar), () -> bankRepo.getAllFor(avatar, page, size), this::toView);
	}

	@Get("/storage")
	@Produces(APPLICATION_JSON)
	public EntryPage<StorageEntryView> storageEntries(@PathVariable(PATH_VAR_AVATAR) String avatar, @QueryValue(value = PAGE, defaultValue = DEFAULT_PAGE) @Min(0) int page,
			@QueryValue(value = SIZE, defaultValue = DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		PageRequest window = new PageRequest(page, size);
		return pageOf(avatar, window, () -> storageRepo.countFor(avatar), () -> storageRepo.getAllFor(avatar, page, size), this::toView);
	}

	private <E, V> EntryPage<V> pageOf(String avatar, PageRequest window, LongSupplier count, Supplier<List<E>> read, Function<E, V> toView) {
		logger.info("Looking for entries for " + avatar);
		long totalCount = count.getAsLong();
		if (totalCount == 0 && isUnknown(avatar)) {
			throw new NoElementFound(avatar);
		}

		List<V> items = window.offset() < totalCount ? read.get().stream().map(toView).toList() : List.of();
		logger.info("Found " + items.size() + " entries.");
		return new EntryPage<>(window.page(), window.size(), totalCount, items);
	}

	private boolean isUnknown(String avatar) {
		return bankRepo.countFor(avatar) == 0 && storageRepo.countFor(avatar) == 0;
	}

	private BankEntryView toView(BankEntry entry) {
		return new BankEntryView(entry.timeStamp(), entry.avatar(), entry.amount(), wireNames.of(entry.type()));
	}

	private StorageEntryView toView(StorageEntry entry) {
		return new StorageEntryView(entry.timeStamp(), entry.avatar(), entry.quantity(), entry.name(), entry.quality(), wireNames.of(entry.type()));
	}
}
