package dev.schoenberg.evergore.protocolParser.rest.controller;

import static io.micronaut.http.MediaType.*;
import static java.lang.String.*;

import java.util.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.rest.controller.OutputFormatter.*;
import io.micronaut.http.annotation.*;

@Controller("/avatars/{avatar}")
public class AvatarController {
	private static final String PATH_VAR_AVATAR = "avatar";

	private static final String QUERY_VAR_PAGE = "page";
	private static final String QUERY_VAR_PAGE_DEFAULT = "0";

	private static final String QUERY_VAR_SIZE = "size";
	private static final String QUERY_VAR_SIZE_DEFAULT = "100";

	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final OutputFormatter formatter;
	private final Logger logger;

	public AvatarController(BankRepository bankRepo, StorageRepository storageRepo, OutputFormatter formatter,
			Logger logger) {
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.formatter = formatter;
		this.logger = logger;
	}

	@Get("/bank")
	@Produces(TEXT_PLAIN)
	public String bankInformation(@PathVariable(value = PATH_VAR_AVATAR) String avatar,
			@QueryValue(value = QUERY_VAR_PAGE, defaultValue = QUERY_VAR_PAGE_DEFAULT) int page,
			@QueryValue(value = QUERY_VAR_SIZE, defaultValue = QUERY_VAR_SIZE_DEFAULT) int size) {

		logger.info("Looking for entries for " + avatar);
		List<BankEntry> all = bankRepo.getAllFor(avatar);
		logger.info("Found " + all.size() + " entries.");

		List<Column<BankEntry>> columns = new ArrayList<>();
		columns.add(new Column<>("TimeStamp", b -> b.timeStamp.toString()));
		columns.add(new Column<>("Avatar", b -> b.avatar));
		columns.add(new Column<>("Amount", b -> valueOf(b.amount)));
		columns.add(new Column<>("TransferType", b -> b.type.toString()));

		return formatter.generateOutput(all, columns);
	}

	@Get("/storage")
	public String storageInformation(@PathVariable(value = PATH_VAR_AVATAR) String avatar,
			@QueryValue(value = QUERY_VAR_PAGE, defaultValue = QUERY_VAR_PAGE_DEFAULT) int page,
			@QueryValue(value = QUERY_VAR_SIZE, defaultValue = QUERY_VAR_SIZE_DEFAULT) int size) {
		logger.info("Looking for entries for " + avatar);
		List<StorageEntry> all = storageRepo.getAllFor(avatar);
		logger.info("Found " + all.size() + " entries.");

		List<Column<StorageEntry>> columns = new ArrayList<>();
		columns.add(new Column<>("TimeStamp", d -> d.timeStamp.toString()));
		columns.add(new Column<>("Avatar", d -> d.avatar));
		columns.add(new Column<>("Quantity", d -> valueOf(d.quantity)));
		columns.add(new Column<>("Name", d -> d.name));
		columns.add(new Column<>("Quality", d -> valueOf(d.quality)));
		columns.add(new Column<>("TransferType", d -> d.type.toString()));

		return formatter.generateOutput(all, columns);
	}
}
