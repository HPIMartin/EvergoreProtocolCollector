package dev.schoenberg.evergore.protocolParser.rest.controller;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static io.micronaut.http.MediaType.*;
import static java.lang.String.*;
import static java.nio.charset.StandardCharsets.*;
import static java.time.format.DateTimeFormatter.*;
import static org.apache.commons.text.StringEscapeUtils.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.rest.controller.OutputFormatter.*;
import io.micronaut.http.annotation.*;

@Controller("/avatars/{" + AvatarController.PATH_VAR_AVATAR + "}")
public class AvatarController {
	public static final String PATH_VAR_AVATAR = "avatar";
	private static final String QUERY_VAR_PAGE = "page";
	private static final String QUERY_VAR_PAGE_DEFAULT = "0";

	private static final long PAGE_SIZE = 100;

	private static final DateTimeFormatter DATE_TIME_PATTERN = ofPattern("dd.MM.yyyy HH:mm");

	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final OutputFormatter formatter;
	private final Logger logger;

	public AvatarController(BankRepository bankRepo, StorageRepository storageRepo, OutputFormatter formatter, Logger logger) {
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.formatter = formatter;
		this.logger = logger;
	}

	@Get("/bank")
	@Produces(TEXT_HTML)
	public String bankInformation(@PathVariable(value = PATH_VAR_AVATAR) String avatar,
			@QueryValue(value = QUERY_VAR_PAGE, defaultValue = QUERY_VAR_PAGE_DEFAULT) int page) {
		String pageWithMeta = getPageTemplateWithMetaData(page, bankRepo.getAllDifferentAvatars());

		logger.info("Looking for entries for " + avatar);
		List<BankEntry> all = bankRepo.getAllFor(avatar, page, PAGE_SIZE);
		logger.info("Found " + all.size() + " entries.");

		List<Column<BankEntry>> columns = new ArrayList<>();
		columns.add(new Column<>("TimeStamp", b -> getLocalDateTimeString(b.timeStamp)));
		columns.add(new Column<>("Avatar", b -> b.avatar));
		columns.add(new Column<>("Amount", b -> valueOf(b.amount)));
		columns.add(new Column<>("TransferType", b -> b.type.toString()));

		return pageWithMeta.replace("###PAGE_CONTENT_PLACEHOLDER###", formatter.createTable(all, columns));
	}

	@Get("/storage")
	@Produces(TEXT_HTML)
	public String storageInformation(@PathVariable(value = PATH_VAR_AVATAR) String avatar,
			@QueryValue(value = QUERY_VAR_PAGE, defaultValue = QUERY_VAR_PAGE_DEFAULT) int page) {

		String pageWithMeta = getPageTemplateWithMetaData(page, storageRepo.getAllDifferentAvatars());

		logger.info("Looking for entries for " + avatar);
		List<StorageEntry> all = storageRepo.getAllFor(avatar, page, PAGE_SIZE);
		logger.info("Found " + all.size() + " entries.");

		List<Column<StorageEntry>> columns = new ArrayList<>();
		columns.add(new Column<>("TimeStamp", d -> getLocalDateTimeString(d.timeStamp)));
		columns.add(new Column<>("Avatar", d -> d.avatar));
		columns.add(new Column<>("Quantity", d -> valueOf(d.quantity)));
		columns.add(new Column<>("Name", d -> d.name));
		columns.add(new Column<>("Quality", d -> valueOf(d.quality)));
		columns.add(new Column<>("TransferType", d -> d.type.toString()));

		return pageWithMeta.replace("###PAGE_CONTENT_PLACEHOLDER###", formatter.createTable(all, columns));
	}

	private String getPageTemplateWithMetaData(int page, List<String> avatars) {
		String pageTemplate = new String(silentThrow(() -> getClass().getResourceAsStream("/static/htmlTemplate.txt").readAllBytes()), UTF_8);

		String avatarsSet = pageTemplate.replace("###AVATARS_PLACEHOLDER###", generateDropDownOptions(avatars));
		return avatarsSet.replace("###PAGE_PLACEHOLDER###", valueOf(page));
	}

	private String generateDropDownOptions(List<String> avatars) {
		return avatars.stream().map(this::formatAsOption).reduce("", (x, y) -> x + "\n" + y);
	}

	private String formatAsOption(String avatar) {
		String escaped = escapeHtml4(avatar);
		return "<option value=\"" + escaped + "\">" + escaped + "</option>";
	}

	private String getLocalDateTimeString(Instant toBeConverted) {
		if (toBeConverted.toString().contains("2023-02-19") && toBeConverted.toString().contains(":17")) {
			System.out.println("Halt STOP!");
		}
		return toBeConverted.atZone(APP_ZONE).toLocalDateTime().format(DATE_TIME_PATTERN);
	}
}
